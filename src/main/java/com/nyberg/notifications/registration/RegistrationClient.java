package com.nyberg.notifications.registration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class RegistrationClient {

    private final RestTemplate restTemplate;
    private final String registryUrl;
    private final String instanceId;
    private final String host;
    private final int port;

    public RegistrationClient(
            RestTemplate restTemplate,
            @Value("${registration.url:http://localhost:8085}") String registryUrl,
            @Value("${registration.instance-id:notifications-1}") String instanceId,
            @Value("${registration.host:localhost}") String host,
            @Value("${server.port:8081}") int port) {
        this.restTemplate = restTemplate;
        this.registryUrl = registryUrl;
        this.instanceId = instanceId;
        this.host = host;
        this.port = port;
    }

    @PostConstruct
    public void register() {
        try {
            Map<String, Object> body = Map.of(
                "serviceName", "notifications",
                "instanceId", instanceId,
                "host", host,
                "port", port,
                "version", "1.0.0",
                "metadata", Map.of("environment", "local")
            );
            post("/register", body);
            log.info("Registered with registration-service as {}", instanceId);
        } catch (Exception e) {
            log.warn("Could not register with registration-service: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${registration.heartbeat-interval-ms:30000}")
    public void heartbeat() {
        try {
            post("/heartbeat", Map.of("instanceId", instanceId, "status", "UP"));
        } catch (Exception e) {
            log.debug("Heartbeat failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void deregister() {
        try {
            HttpHeaders headers = new HttpHeaders();
            restTemplate.exchange(
                registryUrl + "/deregister/" + instanceId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
            );
            log.info("Deregistered from registration-service");
        } catch (Exception e) {
            log.debug("Deregister failed: {}", e.getMessage());
        }
    }

    private void post(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(
            registryUrl + path,
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Void.class
        );
    }
}
