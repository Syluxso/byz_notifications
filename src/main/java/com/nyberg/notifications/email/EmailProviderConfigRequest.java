package com.nyberg.notifications.email;

import java.util.UUID;

public record EmailProviderConfigRequest(
        UUID organizationId,
        String provider,
        String apiKey
) {}
