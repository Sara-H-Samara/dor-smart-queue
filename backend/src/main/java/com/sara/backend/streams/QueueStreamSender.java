package com.sara.backend.streams;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Component
public class QueueStreamSender {

    private static final String READY_EVENT_NAME = "ready";
    private static final String HEARTBEAT_COMMENT = "heartbeat";
    private static final String EMPTY_EVENT_DATA = "{}";

    private final QueueStreamRegistry queueStreamRegistry;

    public QueueStreamSender(QueueStreamRegistry queueStreamRegistry) {
        this.queueStreamRegistry = queueStreamRegistry;
    }

    public void sendReady(SseEmitter emitter) {
        sendOrRemove(emitter, SseEmitter.event().name(READY_EVENT_NAME).data(EMPTY_EVENT_DATA));
    }

    public void sendChange(SseEmitter emitter, QueueChangeKind kind) {
        sendOrRemove(emitter, SseEmitter.event().name(kind.eventName()).data(EMPTY_EVENT_DATA));
    }

    public void sendHeartbeat(SseEmitter emitter) {
        sendOrRemove(emitter, SseEmitter.event().comment(HEARTBEAT_COMMENT));
    }

    private void sendOrRemove(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException closedStream) {
            queueStreamRegistry.remove(emitter);
        }
    }
}
