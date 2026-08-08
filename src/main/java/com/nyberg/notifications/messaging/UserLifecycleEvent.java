package com.nyberg.notifications.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON payload for {@code byz.iam.user}. See events-service {@code docs/EVENTS.md}.
 * Compatible with byz-iam {@code UserLifecycleEvent} (unknown fields ignored).
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
        String resetUrl,
        String provider,
        /** Present for {@link #TYPE_DEVICE_REGISTERED}; null otherwise. */
        UUID deviceId,
        String deviceLabel,
        String deviceIp
) {
    public static final String TYPE_USER_REGISTERED = "user.registered";
    public static final String TYPE_PASSWORD_RESET_REQUESTED = "user.password_reset_requested";
    public static final String TYPE_DEVICE_REGISTERED = "device.registered";
    public static final String TYPE_DEVICE_REVOKED = "device.revoked";
}
