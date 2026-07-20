package com.nyberg.notifications.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "byz.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class IamUserEventListener {

    private final ObjectMapper objectMapper;
    private final UserLifecycleEventHandler handler;

    @KafkaListener(
            topics = "${byz.kafka.topics.iam-user:byz.iam.user}",
            groupId = "${spring.kafka.consumer.group-id:notifications}"
    )
    public void onMessage(String payload) {
        try {
            UserLifecycleEvent event = objectMapper.readValue(payload, UserLifecycleEvent.class);
            handler.handle(event);
        } catch (Exception e) {
            log.error("Failed to process byz.iam.user message: {}", e.toString());
            throw new IllegalStateException("Failed to process byz.iam.user message", e);
        }
    }
}
