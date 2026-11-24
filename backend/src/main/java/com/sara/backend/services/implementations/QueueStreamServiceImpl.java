package com.sara.backend.services.implementations;

import com.sara.backend.services.QueueStreamService;
import com.sara.backend.streams.QueueStreamRegistry;
import com.sara.backend.streams.QueueStreamSender;
import com.sara.backend.streams.StreamAudience;
import com.sara.backend.streams.StreamConnectionLimiter;
import com.sara.backend.streams.StreamReservation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class QueueStreamServiceImpl implements QueueStreamService {

    private final QueueStreamRegistry queueStreamRegistry;
    private final QueueStreamSender queueStreamSender;
    private final StreamConnectionLimiter streamConnectionLimiter;
    private final long streamLifetimeMillis;

    public QueueStreamServiceImpl(
            QueueStreamRegistry queueStreamRegistry,
            QueueStreamSender queueStreamSender,
            StreamConnectionLimiter streamConnectionLimiter,
            @Value("${dor.streams.lifetime-millis:600000}") long streamLifetimeMillis) {
        this.queueStreamRegistry = queueStreamRegistry;
        this.queueStreamSender = queueStreamSender;
        this.streamConnectionLimiter = streamConnectionLimiter;
        this.streamLifetimeMillis = streamLifetimeMillis;
    }

    @Override
    public SseEmitter openPublicStream(String clientAddress) {
        return openStream(StreamAudience.PUBLIC, clientAddress);
    }

    @Override
    public SseEmitter openStudentStream(String clientAddress) {
        return openStream(StreamAudience.STUDENT, clientAddress);
    }

    @Override
    public SseEmitter openStaffStream(String clientAddress) {
        return openStream(StreamAudience.STAFF, clientAddress);
    }

    @Override
    public SseEmitter openAdminStream(String clientAddress) {
        return openStream(StreamAudience.ADMIN, clientAddress);
    }

    private SseEmitter openStream(StreamAudience audience, String clientAddress) {
        StreamReservation reservation = streamConnectionLimiter.reserveSlot(clientAddress);
        SseEmitter emitter = new SseEmitter(streamLifetimeMillis);
        emitter.onCompletion(reservation::release);
        emitter.onCompletion(() -> queueStreamRegistry.remove(emitter));
        emitter.onTimeout(() -> queueStreamRegistry.remove(emitter));
        emitter.onError(streamError -> queueStreamRegistry.remove(emitter));
        queueStreamRegistry.register(audience, emitter);
        queueStreamSender.sendReady(emitter);
        return emitter;
    }
}
