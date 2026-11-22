package com.sara.backend.streams;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class QueueStreamRegistry {

    private final Map<StreamAudience, Set<SseEmitter>> emittersByAudience = new EnumMap<>(StreamAudience.class);

    public QueueStreamRegistry() {
        for (StreamAudience audience : StreamAudience.values()) {
            emittersByAudience.put(audience, new CopyOnWriteArraySet<>());
        }
    }

    public void register(StreamAudience audience, SseEmitter emitter) {
        emittersByAudience.get(audience).add(emitter);
    }

    public void remove(SseEmitter emitter) {
        emittersByAudience.values().forEach(emitters -> emitters.remove(emitter));
    }

    public List<SseEmitter> findAllEmitters() {
        return emittersByAudience.values().stream()
                .flatMap(Set::stream)
                .toList();
    }

    public List<SseEmitter> findEmittersFor(QueueChangeKind kind) {
        return Arrays.stream(StreamAudience.values())
                .filter(audience -> audience.receives(kind))
                .flatMap(audience -> emittersByAudience.get(audience).stream())
                .toList();
    }
}
