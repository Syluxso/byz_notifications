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
public class NotificationIntentListener {

    private final ObjectMapper objectMapper;
    private final NotificationIntentHandler handler;

    @KafkaListener(
            topics = "${byz.kafka.topics.notification-intent:byz.notifications.intent}",
            groupId = "${spring.kafka.consumer.group-id:notifications}"
    )
    public void onMessage(String payload) {
        try {
            NotificationIntentEvent event = objectMapper.readValue(payload, NotificationIntentEvent.class);
            log.info("Received byz.notifications.intent kind={} eventId={} userId={}",
                    event.kind(), event.eventId(), event.userId());
            handler.handle(event);
        } catch (Exception e) {
            log.error("Failed to process byz.notifications.intent: {} payloadSnippet={}",
                    e.toString(),
                    payload != null && payload.length() > 400 ? payload.substring(0, 400) + "…" : payload);
            throw new IllegalStateException("Failed to process byz.notifications.intent message", e);
        }
    }
}
