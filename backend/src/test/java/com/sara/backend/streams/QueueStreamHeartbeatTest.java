package com.sara.backend.streams;

import com.sara.backend.support.RecordingSseEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Queue stream heartbeat")
class QueueStreamHeartbeatTest {

    private static final String HEARTBEAT_EVENT = ":heartbeat\n\n";

    private QueueStreamRegistry queueStreamRegistry;
    private QueueStreamHeartbeat queueStreamHeartbeat;

    @BeforeEach
    void createHeartbeat() {
        queueStreamRegistry = new QueueStreamRegistry();
        queueStreamHeartbeat = new QueueStreamHeartbeat(
                queueStreamRegistry, new QueueStreamSender(queueStreamRegistry));
    }

    @Test
    void sendHeartbeatToAllStreams_withEmittersOfAllAudiences_sendsOneHeartbeatToEach() {
        RecordingSseEmitter publicEmitter = registerEmitter(StreamAudience.PUBLIC);
        RecordingSseEmitter studentEmitter = registerEmitter(StreamAudience.STUDENT);
        RecordingSseEmitter staffEmitter = registerEmitter(StreamAudience.STAFF);
        RecordingSseEmitter adminEmitter = registerEmitter(StreamAudience.ADMIN);

        queueStreamHeartbeat.sendHeartbeatToAllStreams();

        for (RecordingSseEmitter emitter : List.of(publicEmitter, studentEmitter, staffEmitter, adminEmitter)) {
            assertEquals(List.of(HEARTBEAT_EVENT), emitter.writtenEvents());
        }
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
}
