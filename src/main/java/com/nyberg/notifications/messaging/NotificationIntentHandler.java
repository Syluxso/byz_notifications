package com.nyberg.notifications.messaging;

import com.nyberg.notifications.email.EmailSendRequest;
import com.nyberg.notifications.email.EmailSendService;
import com.nyberg.notifications.notification.dto.InAppNotificationRequest;
import com.nyberg.notifications.notification.service.NotificationService;
import com.nyberg.notifications.tenant.OrganizationContext;
import com.nyberg.notifications.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationIntentHandler {

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;
    private final EmailSendService emailSendService;

    @Transactional
    public void handle(NotificationIntentEvent event) {
        if (event.eventId() == null) {
            log.warn("Ignoring notification intent without eventId");
            return;
        }
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping already-processed intent eventId={}", event.eventId());
            return;
        }

        if (event.organizationId() == null || event.userId() == null) {
            log.warn("Ignoring notification intent missing org/user eventId={}", event.eventId());
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(event.eventId())
                    .eventType(event.type() != null ? event.type() : NotificationIntentEvent.TYPE)
                    .build());
            return;
        }

        List<String> channels = event.channels() != null && !event.channels().isEmpty()
                ? event.channels()
                : List.of("in_app");

        if (channels.stream().anyMatch(c -> "in_app".equalsIgnoreCase(c))) {
            deliverInApp(event);
        }
        if (channels.stream().anyMatch(c -> "email".equalsIgnoreCase(c))) {
            deliverEmail(event);
        }

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.eventId())
                .eventType(event.kind() != null ? event.kind() : NotificationIntentEvent.TYPE)
                .build());
    }

    private void deliverInApp(NotificationIntentEvent event) {
        String title = blankTo(event.title(), "Notification");
        String message = blankTo(event.message(), "");
        OrganizationContext.set(event.organizationId());
        TenantContext.set(event.tenantId());
        try {
            notificationService.createInApp(new InAppNotificationRequest(
                    event.organizationId(),
                    event.userId(),
                    title,
                    message.isBlank() ? title : message,
                    event.link(),
                    event.data() != null ? event.data() : Map.of(
                            "eventId", event.eventId().toString(),
                            "kind", event.kind() != null ? event.kind() : ""
                    ),
                    event.priority() != null ? event.priority() : "normal",
                    event.source() != null ? event.source() : "byz.notifications.intent"
            ));
            log.info("In-app from intent eventId={} userId={} kind={}",
                    event.eventId(), event.userId(), event.kind());
        } catch (Exception e) {
            log.warn("In-app intent failed eventId={}: {}", event.eventId(), e.toString());
        } finally {
            OrganizationContext.clear();
            TenantContext.clear();
        }
    }

    private void deliverEmail(NotificationIntentEvent event) {
        if (event.email() == null || event.email().isBlank()) {
            log.info("Email channel skipped — no email on intent eventId={}", event.eventId());
            return;
        }
        String display = blankTo(event.displayName(), "there");
        String title = blankTo(event.title(), "Notification");
        String message = blankTo(event.message(), "");
        String link = event.link();
        String safeName = escapeHtml(display);
        String safeTitle = escapeHtml(title);
        String safeMsg = escapeHtml(message);
        String html = ""
                + "<div style=\"font-family:Helvetica,Arial,sans-serif;line-height:1.5;color:#151A20;\">"
                + "<p>Hi " + safeName + ",</p>"
                + "<p><strong>" + safeTitle + "</strong></p>"
                + (safeMsg.isBlank() ? "" : "<p>" + safeMsg + "</p>")
                + (link != null && !link.isBlank()
                ? "<p><a href=\"" + escapeHtml(link.startsWith("http") ? link : "https://app.byzantineapp.com" + link) + "\">Open in Byzantine</a></p>"
                : "")
                + "<p style=\"margin-top:24px;color:#5C6874;font-size:13px;\">— The Byzantine team</p>"
                + "</div>";
        try {
            emailSendService.send(new EmailSendRequest(
                    event.organizationId(),
                    event.email(),
                    display,
                    title,
                    html,
                    event.userId(),
                    event.tenantId(),
                    event.source() != null ? event.source() : "byz.notifications.intent",
                    event.priority() != null ? event.priority() : "normal"
            ));
            log.info("Email from intent eventId={} to={}", event.eventId(), event.email());
        } catch (Exception e) {
            log.warn("Email intent skipped eventId={} (configure org SendGrid in Admin): {}",
                    event.eventId(), e.toString());
        }
    }

    private static String blankTo(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private static String escapeHtml(String raw) {
        if (raw == null) return "";
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
