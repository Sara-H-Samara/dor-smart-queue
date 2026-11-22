package com.sara.backend.streams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Queue stream registry")
class QueueStreamRegistryTest {

    private QueueStreamRegistry queueStreamRegistry;

    @BeforeEach
    void createRegistry() {
        queueStreamRegistry = new QueueStreamRegistry();
    }

    @Test
    void findEmittersFor_withNoRegisteredEmitters_returnsEmptyList() {
        assertTrue(queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED).isEmpty());
    }

    @Test
    void findEmittersFor_ticketChanged_returnsPublicStudentAndStaffEmitters() {
        SseEmitter publicEmitter = registerEmitter(StreamAudience.PUBLIC);
        SseEmitter studentEmitter = registerEmitter(StreamAudience.STUDENT);
        SseEmitter staffEmitter = registerEmitter(StreamAudience.STAFF);

        assertEquals(
                Set.of(publicEmitter, studentEmitter, staffEmitter),
                Set.copyOf(queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED)));
    }

    @Test
    void findEmittersFor_ticketChanged_excludesAdminEmitter() {
        SseEmitter adminEmitter = registerEmitter(StreamAudience.ADMIN);

        assertFalse(queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED).contains(adminEmitter));
    }

    @Test
    void findEmittersFor_configurationChanged_returnsEmittersOfAllFourAudiences() {
        SseEmitter publicEmitter = registerEmitter(StreamAudience.PUBLIC);
        SseEmitter studentEmitter = registerEmitter(StreamAudience.STUDENT);
        SseEmitter staffEmitter = registerEmitter(StreamAudience.STAFF);
        SseEmitter adminEmitter = registerEmitter(StreamAudience.ADMIN);

        assertEquals(
                Set.of(publicEmitter, studentEmitter, staffEmitter, adminEmitter),
                Set.copyOf(queueStreamRegistry.findEmittersFor(QueueChangeKind.CONFIGURATION_CHANGED)));
    }

    @Test
    void findEmittersFor_twoEmittersOfSameAudience_returnsBoth() {
        registerEmitter(StreamAudience.PUBLIC);
        registerEmitter(StreamAudience.PUBLIC);

        assertEquals(2, queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED).size());
    }

    @Test
    void remove_registeredEmitter_noLongerReturned() {
        SseEmitter removedEmitter = registerEmitter(StreamAudience.STUDENT);
        SseEmitter keptEmitter = registerEmitter(StreamAudience.STUDENT);

        queueStreamRegistry.remove(removedEmitter);

        assertEquals(List.of(keptEmitter),
                queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED));
    }

    @Test
    void remove_unknownEmitter_changesNothing() {
        SseEmitter keptEmitter = registerEmitter(StreamAudience.PUBLIC);

        queueStreamRegistry.remove(new SseEmitter());

        assertEquals(List.of(keptEmitter),
                queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED));
    }

    private SseEmitter registerEmitter(StreamAudience audience) {
        SseEmitter emitter = new SseEmitter();
        queueStreamRegistry.register(audience, emitter);
        return emitter;
    }
}
