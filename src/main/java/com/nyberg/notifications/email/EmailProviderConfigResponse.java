package com.nyberg.notifications.email;

import java.time.Instant;
import java.util.UUID;

public record EmailProviderConfigResponse(
        UUID id,
        UUID organizationId,
        String provider,
        String keyHint,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
