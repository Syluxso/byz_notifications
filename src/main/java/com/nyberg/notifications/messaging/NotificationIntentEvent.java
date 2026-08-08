package com.nyberg.notifications.messaging;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Product-layer notification intent (typically from byz-managed-api).
 * Topic: {@code byz.notifications.intent}
 */
public record NotificationIntentEvent(
        UUID eventId,
        String type,
        Instant occurredAt,
        UUID organizationId,
        UUID tenantId,
        UUID userId,
        String email,
        String displayName,
        String title,
        String message,
        String link,
        List<String> channels,
        Map<String, Object> data,
        String priority,
        String source,
        String kind
) {
    public static final String TYPE = "notification.intent";
}
