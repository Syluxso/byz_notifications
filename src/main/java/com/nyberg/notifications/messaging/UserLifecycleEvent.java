package com.nyberg.notifications.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON payload for {@code byz.iam.user}. See events-service {@code docs/EVENTS.md}.
 */
public record UserLifecycleEvent(
        UUID eventId,
        String type,
        Instant occurredAt,
        UUID organizationId,
        UUID tenantId,
        UUID userId,
        String email,
        String displayName,
        /** Present for {@link #TYPE_PASSWORD_RESET_REQUESTED}; null otherwise. */
        String resetUrl
) {
    public static final String TYPE_USER_REGISTERED = "user.registered";
    public static final String TYPE_PASSWORD_RESET_REQUESTED = "user.password_reset_requested";
}
