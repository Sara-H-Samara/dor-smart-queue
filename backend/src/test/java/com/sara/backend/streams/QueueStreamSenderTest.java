package com.sara.backend.streams;

import com.sara.backend.support.RecordingSseEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Queue stream sender")
class QueueStreamSenderTest {

    private QueueStreamRegistry queueStreamRegistry;
    private QueueStreamSender queueStreamSender;

    @BeforeEach
    void createSender() {
        queueStreamRegistry = new QueueStreamRegistry();
        queueStreamSender = new QueueStreamSender(queueStreamRegistry);
    }

    @Test
    void sendReady_openEmitter_writesReadyEventWithEmptyData() {
        RecordingSseEmitter emitter = new RecordingSseEmitter();

        queueStreamSender.sendReady(emitter);

        assertEquals(List.of("event:ready\ndata:{}\n\n"), emitter.writtenEvents());
    }

    @Test
    void sendChange_ticketChanged_writesTicketChangedEventWithEmptyData() {
        RecordingSseEmitter emitter = new RecordingSseEmitter();

        queueStreamSender.sendChange(emitter, QueueChangeKind.TICKET_CHANGED);

        assertEquals(List.of("event:ticket-changed\ndata:{}\n\n"), emitter.writtenEvents());
    }

    @Test
    void sendChange_configurationChanged_writesConfigurationChangedEventWithEmptyData() {
        RecordingSseEmitter emitter = new RecordingSseEmitter();

        queueStreamSender.sendChange(emitter, QueueChangeKind.CONFIGURATION_CHANGED);

        assertEquals(List.of("event:configuration-changed\ndata:{}\n\n"), emitter.writtenEvents());
    }

    @Test
    void sendHeartbeat_openEmitter_writesCommentLine() {
        RecordingSseEmitter emitter = new RecordingSseEmitter();

        queueStreamSender.sendHeartbeat(emitter);

        assertEquals(List.of(":heartbeat\n\n"), emitter.writtenEvents());
    }

    @Test
    void sendChange_emitterThatFailsToWrite_removesEmitterFromRegistry() {
        FailingEmitter brokenEmitter = new FailingEmitter(new IOException("Broken pipe"));
        queueStreamRegistry.register(StreamAudience.PUBLIC, brokenEmitter);

        queueStreamSender.sendChange(brokenEmitter, QueueChangeKind.TICKET_CHANGED);

        assertTrue(queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED).isEmpty());
    }

    @Test
    void sendChange_emitterAlreadyCompleted_removesEmitterFromRegistry() {
        FailingEmitter completedEmitter = new FailingEmitter(new IllegalStateException("Already completed"));
        queueStreamRegistry.register(StreamAudience.STUDENT, completedEmitter);

        queueStreamSender.sendChange(completedEmitter, QueueChangeKind.TICKET_CHANGED);

        assertTrue(queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED).isEmpty());
    }

    @Test
    void sendChange_emitterThatFailsToWrite_keepsOtherEmittersRegistered() {
        FailingEmitter brokenEmitter = new FailingEmitter(new IOException("Broken pipe"));
        RecordingSseEmitter healthyEmitter = new RecordingSseEmitter();
        queueStreamRegistry.register(StreamAudience.PUBLIC, brokenEmitter);
        queueStreamRegistry.register(StreamAudience.PUBLIC, healthyEmitter);

        queueStreamSender.sendChange(brokenEmitter, QueueChangeKind.TICKET_CHANGED);

        assertEquals(List.of(healthyEmitter), queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED));
    }

    private static class FailingEmitter extends SseEmitter {

        private final Exception failure;

        FailingEmitter(Exception failure) {
            this.failure = failure;
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            if (failure instanceof IOException ioFailure) {
                throw ioFailure;
            }
            throw (RuntimeException) failure;
        }
    }
}
