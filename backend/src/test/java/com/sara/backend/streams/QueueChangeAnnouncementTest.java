package com.sara.backend.streams;

import com.sara.backend.support.RecordingSseEmitter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Queue change announcements")
class QueueChangeAnnouncementTest {

    private static final String TICKET_CHANGED_EVENT = "event:ticket-changed\ndata:{}\n\n";
    private static final String CONFIGURATION_CHANGED_EVENT = "event:configuration-changed\ndata:{}\n\n";
    private static final long EVENT_WAIT_MILLIS = 2000;
    private static final long NO_EVENT_WAIT_MILLIS = 300;

    @Autowired
    private QueueChangeAnnouncer queueChangeAnnouncer;

    @Autowired
    private QueueStreamRegistry queueStreamRegistry;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private RecordingSseEmitter publicEmitter;
    private RecordingSseEmitter studentEmitter;
    private RecordingSseEmitter staffEmitter;
    private RecordingSseEmitter adminEmitter;

    @BeforeEach
    void registerOneEmitterForEachAudience() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        publicEmitter = registerEmitter(StreamAudience.PUBLIC);
        studentEmitter = registerEmitter(StreamAudience.STUDENT);
        staffEmitter = registerEmitter(StreamAudience.STAFF);
        adminEmitter = registerEmitter(StreamAudience.ADMIN);
    }

    @AfterEach
    void removeRegisteredEmitters() {
        List.of(publicEmitter, studentEmitter, staffEmitter, adminEmitter).forEach(queueStreamRegistry::remove);
    }

    @Test
    void announceTicketChanged_afterCommit_sendsEmptyTicketChangedEventToPublicStudentAndStaff() {
        transactionTemplate.executeWithoutResult(status -> queueChangeAnnouncer.announceTicketChanged());

        assertEquals(TICKET_CHANGED_EVENT, publicEmitter.pollEvent(EVENT_WAIT_MILLIS));
        assertEquals(TICKET_CHANGED_EVENT, studentEmitter.pollEvent(EVENT_WAIT_MILLIS));
        assertEquals(TICKET_CHANGED_EVENT, staffEmitter.pollEvent(EVENT_WAIT_MILLIS));
    }

    @Test
    void announceTicketChanged_afterCommit_doesNotSendToAdmin() {
        transactionTemplate.executeWithoutResult(status -> queueChangeAnnouncer.announceTicketChanged());

        assertEquals(TICKET_CHANGED_EVENT, publicEmitter.pollEvent(EVENT_WAIT_MILLIS));
        assertNull(adminEmitter.pollEvent(NO_EVENT_WAIT_MILLIS));
    }

    @Test
    void announceConfigurationChanged_afterCommit_sendsEmptyEventToAllFourAudiences() {
        transactionTemplate.executeWithoutResult(status -> queueChangeAnnouncer.announceConfigurationChanged());

        assertEquals(CONFIGURATION_CHANGED_EVENT, publicEmitter.pollEvent(EVENT_WAIT_MILLIS));
        assertEquals(CONFIGURATION_CHANGED_EVENT, studentEmitter.pollEvent(EVENT_WAIT_MILLIS));
        assertEquals(CONFIGURATION_CHANGED_EVENT, staffEmitter.pollEvent(EVENT_WAIT_MILLIS));
        assertEquals(CONFIGURATION_CHANGED_EVENT, adminEmitter.pollEvent(EVENT_WAIT_MILLIS));
    }

    @Test
    void announceTicketChanged_beforeCommit_sendsNothingUntilTheTransactionCommits() {
        transactionTemplate.executeWithoutResult(status -> {
            queueChangeAnnouncer.announceTicketChanged();
            assertNull(publicEmitter.pollEvent(NO_EVENT_WAIT_MILLIS));
        });

        assertEquals(TICKET_CHANGED_EVENT, publicEmitter.pollEvent(EVENT_WAIT_MILLIS));
    }

    @Test
    void announceTicketChanged_whenTransactionRollsBack_sendsNothing() {
        transactionTemplate.executeWithoutResult(status -> {
            queueChangeAnnouncer.announceTicketChanged();
            status.setRollbackOnly();
        });
        queueChangeAnnouncer.announceConfigurationChanged();

        assertEquals(CONFIGURATION_CHANGED_EVENT, publicEmitter.pollEvent(EVENT_WAIT_MILLIS));
        assertNull(publicEmitter.pollEvent(NO_EVENT_WAIT_MILLIS));
    }

    @Test
    void announceTicketChanged_withoutTransaction_sendsImmediately() {
        queueChangeAnnouncer.announceTicketChanged();

        assertEquals(TICKET_CHANGED_EVENT, publicEmitter.pollEvent(EVENT_WAIT_MILLIS));
    }

    private RecordingSseEmitter registerEmitter(StreamAudience audience) {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        queueStreamRegistry.register(audience, emitter);
        return emitter;
    }
}
