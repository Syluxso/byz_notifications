package com.nyberg.notifications.observability;

import java.util.Map;

public record ObsLogEntry(
    String timestamp,
    String service,
    String level,
    String eventType,
    String tenantId,
    String userId,
    String correlationId,
    String message,
    Long durationMs,
    Map<String, Object> details
) {
    public static ObsLogEntry of(String level, String eventType, String message,
                                 String tenantId, String userId, Long durationMs,
                                 Map<String, Object> details) {
        return new ObsLogEntry(
            null, "notifications", level, eventType,
            tenantId, userId, CorrelationFilter.current(),
            message, durationMs, details
        );
    }
}
