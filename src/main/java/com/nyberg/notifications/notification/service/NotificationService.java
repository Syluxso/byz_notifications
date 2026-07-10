package com.nyberg.notifications.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyberg.notifications.notification.dto.InAppNotificationRequest;
import com.nyberg.notifications.notification.dto.NotificationResponse;
import com.nyberg.notifications.notification.dto.UnreadCountResponse;
import com.nyberg.notifications.notification.entity.Notification;
import com.nyberg.notifications.notification.repository.NotificationRepository;
import com.nyberg.notifications.observability.LogClient;
import com.nyberg.notifications.tenant.OrganizationContext;
import com.nyberg.notifications.tenant.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final SseService sseService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final LogClient logClient;

    @Transactional
    public NotificationResponse createInApp(InAppNotificationRequest req) {
        UUID orgId = req.organizationId() != null ? req.organizationId() : OrganizationContext.get();
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization ID required — not in request or JWT");
        }
        UUID tenantId = TenantContext.get();

        Notification n = Notification.builder()
                .organizationId(orgId)
                .tenantId(tenantId)
                .userId(req.userId())
                .title(req.title())
                .message(req.message())
                .link(req.link())
                .data(req.data() != null ? toJson(req.data()) : null)
                .source(req.source())
                .priority(req.priority() != null ? req.priority() : "normal")
                .build();

        repository.save(n);

        long unread = repository.countByUserIdAndStatus(req.userId(), "unread");
        sseService.send(req.userId(), Map.of(
                "type", "new_notification",
                "notification", NotificationResponse.from(n),
                "unreadCount", unread
        ));

        meterRegistry.counter("notifications.created", "channel", "in_app",
                "tenant", tenantId != null ? tenantId.toString() : "system").increment();
        log.info("notification created id={} userId={} tenant={}", n.getId(), req.userId(), tenantId);

        logClient.info("notification", "Notification created: " + req.title(),
                tenantId != null ? tenantId.toString() : null,
                req.userId().toString(), null,
                java.util.Map.of("title", req.title(), "source", req.source() != null ? req.source() : ""));

        return NotificationResponse.from(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Notification> results = status != null
                ? repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
                : repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return results.map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(repository.countByUserIdAndStatus(userId, "unread"));
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId, UUID userId) {
        Notification n = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        n.markRead();
        return NotificationResponse.from(repository.save(n));
    }

    @Transactional
    public void markAllRead(UUID userId) {
        int updated = repository.markAllReadByUserId(userId, Instant.now());
        log.info("marked all read userId={} count={}", userId, updated);
    }

    @Transactional
    public void softDelete(UUID notificationId, UUID userId) {
        Notification n = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        n.softDelete();
        repository.save(n);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON in data field");
        }
    }
}
