package com.nyberg.notifications.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record InAppNotificationRequest(
        @NotNull UUID organizationId,
        @NotNull UUID userId,
        @NotBlank String title,
        @NotBlank String message,
        String link,
        Map<String, Object> data,
        String priority,
        String source
) {}
