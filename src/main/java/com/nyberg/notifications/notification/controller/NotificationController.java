package com.nyberg.notifications.notification.controller;

import com.nyberg.notifications.notification.dto.InAppNotificationRequest;
import com.nyberg.notifications.notification.dto.NotificationResponse;
import com.nyberg.notifications.notification.dto.UnreadCountResponse;
import com.nyberg.notifications.notification.service.NotificationService;
import com.nyberg.notifications.notification.service.SseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/notifications", "/api/notifications"}) // /api/notifications = deprecated alias
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseService sseService;

    @PostMapping("/in-app")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse createInApp(@Valid @RequestBody InAppNotificationRequest req) {
        return notificationService.createInApp(req);
    }

    @GetMapping
    public Page<NotificationResponse> list(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = (UUID) auth.getPrincipal();
        return notificationService.list(userId, status, page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return notificationService.unreadCount(userId);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return sseService.subscribe(userId);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return notificationService.markRead(id, userId);
    }

    @PostMapping("/mark-all-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        notificationService.markAllRead(userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        notificationService.softDelete(id, userId);
    }
}
