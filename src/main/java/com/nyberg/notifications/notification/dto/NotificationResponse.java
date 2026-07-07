package com.nyberg.notifications.notification.dto;

import com.nyberg.notifications.notification.entity.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        String channel,
        String status,
        String title,
        String message,
        String link,
        String data,
        String source,
        String priority,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getTenantId(), n.getUserId(),
                n.getChannel(), n.getStatus(),
                n.getTitle(), n.getMessage(), n.getLink(),
                n.getData(), n.getSource(), n.getPriority(),
                n.getReadAt(), n.getCreatedAt()
        );
    }
}
