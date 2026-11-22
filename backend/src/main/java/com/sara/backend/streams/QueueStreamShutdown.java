package com.sara.backend.streams;

import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class QueueStreamShutdown {

    private final QueueStreamRegistry queueStreamRegistry;

    public QueueStreamShutdown(QueueStreamRegistry queueStreamRegistry) {
        this.queueStreamRegistry = queueStreamRegistry;
    }

    @EventListener(ContextClosedEvent.class)
    public void completeAllStreams() {
        queueStreamRegistry.findAllEmitters().forEach(emitter -> {
            queueStreamRegistry.remove(emitter);
            emitter.complete();
        });
    }
}
