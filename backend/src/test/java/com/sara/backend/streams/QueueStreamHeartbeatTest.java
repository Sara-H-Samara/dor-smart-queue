package com.sara.backend.streams;

import com.sara.backend.support.RecordingSseEmitter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Queue stream heartbeat")
class QueueStreamHeartbeatTest {

    private static final String HEARTBEAT_EVENT = ":heartbeat\n\n";

    private static final long EVENT_WAIT_MILLIS = 2000;
    private static final long NO_EVENT_WAIT_MILLIS = 100;

    private QueueStreamRegistry queueStreamRegistry;
    private QueueStreamSendExecutor queueStreamSendExecutor;
    private QueueStreamHeartbeat queueStreamHeartbeat;

    @BeforeEach
    void createHeartbeat() {
        queueStreamRegistry = new QueueStreamRegistry();
        queueStreamSendExecutor = new QueueStreamSendExecutor();
        queueStreamHeartbeat = new QueueStreamHeartbeat(
                queueStreamRegistry, new QueueStreamSender(queueStreamRegistry), queueStreamSendExecutor);
    }

    @AfterEach
    void stopSendExecutor() {
        queueStreamSendExecutor.stopSending();
    }

    @Test
    void sendHeartbeatToAllStreams_withEmittersOfAllAudiences_sendsOneHeartbeatToEach() {
        RecordingSseEmitter publicEmitter = registerEmitter(StreamAudience.PUBLIC);
        RecordingSseEmitter studentEmitter = registerEmitter(StreamAudience.STUDENT);
        RecordingSseEmitter staffEmitter = registerEmitter(StreamAudience.STAFF);
        RecordingSseEmitter adminEmitter = registerEmitter(StreamAudience.ADMIN);

        queueStreamHeartbeat.sendHeartbeatToAllStreams();

        for (RecordingSseEmitter emitter : List.of(publicEmitter, studentEmitter, staffEmitter, adminEmitter)) {
            assertEquals(HEARTBEAT_EVENT, emitter.pollEvent(EVENT_WAIT_MILLIS));
            assertNull(emitter.pollEvent(NO_EVENT_WAIT_MILLIS));
        }
    }

    @Test
    void sendHeartbeatToAllStreams_withOneBlockedEmitter_stillSendsToOtherEmitters() {
        BlockingEmitter blockedEmitter = new BlockingEmitter();
        RecordingSseEmitter healthyEmitter = registerEmitter(StreamAudience.PUBLIC);
        queueStreamRegistry.register(StreamAudience.PUBLIC, blockedEmitter);

        queueStreamHeartbeat.sendHeartbeatToAllStreams();

        assertEquals(HEARTBEAT_EVENT, healthyEmitter.pollEvent(EVENT_WAIT_MILLIS));
        blockedEmitter.release();
    }

    @Test
    void sendHeartbeatToAllStreams_withNoEmitters_sendsNothingAndDoesNotFail() {
        queueStreamHeartbeat.sendHeartbeatToAllStreams();
    }

    private RecordingSseEmitter registerEmitter(StreamAudience audience) {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        queueStreamRegistry.register(audience, emitter);
        return emitter;
    }

    private static class BlockingEmitter extends SseEmitter {

        private final CountDownLatch released = new CountDownLatch(1);

        @Override
        public synchronized void send(SseEventBuilder builder) {
            try {
                released.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        void release() {
            released.countDown();
        }
    }
}
