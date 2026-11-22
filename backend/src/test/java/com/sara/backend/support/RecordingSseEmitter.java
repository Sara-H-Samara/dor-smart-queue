package com.sara.backend.support;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RecordingSseEmitter extends SseEmitter {

    private final BlockingQueue<String> writtenEvents = new LinkedBlockingQueue<>();

    @Override
    public synchronized void send(SseEventBuilder builder) {
        writtenEvents.add(builder.build().stream()
                .map(ResponseBodyEmitter.DataWithMediaType::getData)
                .map(Object::toString)
                .collect(Collectors.joining()));
    }

    public List<String> writtenEvents() {
        return List.copyOf(writtenEvents);
    }

    public String pollEvent(long timeoutMillis) {
        try {
            return writtenEvents.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a stream event.", interrupted);
        }
    }
}
