package com.nyberg.notifications.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class LogClient {

    private final RestTemplate restTemplate;
    private final String observabilityUrl;

    public LogClient(RestTemplate restTemplate,
                     @Value("${observability.url:http://localhost:8084}") String observabilityUrl) {
        this.restTemplate = restTemplate;
        this.observabilityUrl = observabilityUrl;
    }

    public void send(ObsLogEntry entry) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                restTemplate.exchange(
                    observabilityUrl + "/logs",
                    HttpMethod.POST,
                    new HttpEntity<>(entry, headers),
                    Void.class
                );
            } catch (Exception e) {
                log.debug("Observability log failed: {}", e.getMessage());
            }
        });
    }

    public void info(String eventType, String message, String tenantId, String userId,
                     Long durationMs, Map<String, Object> details) {
        send(ObsLogEntry.of("INFO", eventType, message, tenantId, userId, durationMs, details));
    }

    public void warn(String eventType, String message, String tenantId, String userId,
                     Long durationMs, Map<String, Object> details) {
        send(ObsLogEntry.of("WARN", eventType, message, tenantId, userId, durationMs, details));
    }

    public void error(String eventType, String message, String tenantId, String userId,
                      Long durationMs, Map<String, Object> details) {
        send(ObsLogEntry.of("ERROR", eventType, message, tenantId, userId, durationMs, details));
    }
}
