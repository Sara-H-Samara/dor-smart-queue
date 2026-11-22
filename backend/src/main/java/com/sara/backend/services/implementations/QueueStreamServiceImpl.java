package com.sara.backend.services.implementations;

import com.sara.backend.services.QueueStreamService;
import com.sara.backend.streams.QueueStreamRegistry;
import com.sara.backend.streams.QueueStreamSender;
import com.sara.backend.streams.StreamAudience;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;

@Service
public class QueueStreamServiceImpl implements QueueStreamService {

    private static final Duration STREAM_LIFETIME = Duration.ofMinutes(10);

    private final QueueStreamRegistry queueStreamRegistry;
    private final QueueStreamSender queueStreamSender;

    public QueueStreamServiceImpl(QueueStreamRegistry queueStreamRegistry, QueueStreamSender queueStreamSender) {
        this.queueStreamRegistry = queueStreamRegistry;
        this.queueStreamSender = queueStreamSender;
    }

    @Override
    public SseEmitter openPublicStream() {
        return openStream(StreamAudience.PUBLIC);
    }

    @Override
    public SseEmitter openStudentStream() {
        return openStream(StreamAudience.STUDENT);
    }

    @Override
    public SseEmitter openStaffStream() {
        return openStream(StreamAudience.STAFF);
    }

    @Override
    public SseEmitter openAdminStream() {
        return openStream(StreamAudience.ADMIN);
    }

    private SseEmitter openStream(StreamAudience audience) {
        SseEmitter emitter = new SseEmitter(STREAM_LIFETIME.toMillis());
        emitter.onCompletion(() -> queueStreamRegistry.remove(emitter));
        emitter.onTimeout(() -> queueStreamRegistry.remove(emitter));
        emitter.onError(streamError -> queueStreamRegistry.remove(emitter));
        queueStreamRegistry.register(audience, emitter);
        queueStreamSender.sendReady(emitter);
        return emitter;
    }
}
