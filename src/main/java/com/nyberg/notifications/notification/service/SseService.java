package com.nyberg.notifications.notification.service;

import com.nyberg.notifications.observability.LogClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final LogClient logClient;
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        log.info("SSE subscribed userId={}", userId);
        logClient.info("system_action", "SSE client connected userId=" + userId,
                null, userId.toString(), null, Map.of("active_connections", connectionCount()));
        return emitter;
    }

    public void send(UUID userId, Object payload) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) return;

        List<SseEmitter> dead = new java.util.ArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        userEmitters.removeAll(dead);
        if (!dead.isEmpty()) {
            logClient.info("system_action", "SSE dead emitters pruned userId=" + userId + " count=" + dead.size(),
                    null, userId.toString(), null, Map.of("dead_count", dead.size()));
        }
    }

    private int connectionCount() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        emitters.forEach((userId, list) -> {
            List<SseEmitter> dead = new java.util.ArrayList<>();
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    dead.add(emitter);
                }
            }
            list.removeAll(dead);
        });
    }

    private void remove(UUID userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            log.info("SSE disconnected userId={}", userId);
        }
    }
}
