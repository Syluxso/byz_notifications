package com.nyberg.notifications.email;

import java.util.UUID;

import java.util.Map;

public record EmailProviderConfigRequest(
        UUID organizationId,
        String provider,
        Map<String, String> credentials
) {}
