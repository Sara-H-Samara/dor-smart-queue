package com.sara.backend.streams;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.support.OpenStreams;
import com.sara.backend.support.StreamTestHelper;
import com.sara.backend.support.TicketTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.sara.backend.support.StreamTestHelper.EVENT_WAIT_MILLIS;
import static com.sara.backend.support.StreamTestHelper.TICKET_CHANGED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Staff desk changes on a real server")
class StaffDeskStreamEventsTest {

    private static final String STUDENT_EMAIL = "student@example.com";

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private QueueStreamRegistry queueStreamRegistry;

    @Autowired
    private QueueChangeAnnouncer queueChangeAnnouncer;

    private TicketTestHelper ticketTestHelper;
    private StreamTestHelper streamTestHelper;
    private ServiceType registration;
    private Counter deskOne;
    private String studentToken;
    private String staffToken;
    private String adminToken;

    @BeforeEach
    void resetDatabaseAndLogInAllRoles() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        streamTestHelper = new StreamTestHelper(queueStreamRegistry);
        streamTestHelper.removeAllEmitters();
        ticketTestHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        registration = ticketTestHelper.createActiveService("Registration", "R");
        deskOne = ticketTestHelper.createCounter("Desk 1", registration);
        studentToken = ticketTestHelper.registerStudentAndLogin(STUDENT_EMAIL);
        staffToken = ticketTestHelper.loginAsNewStaff();
        adminToken = ticketTestHelper.loginAsAdmin();
    }

    @AfterEach
    void closeOpenStreams() {
        streamTestHelper.close();
    }

    @Test
    void callNext_withWaitingStudent_sendsTicketChangedToPublicStudentAndStaff() throws Exception {
        ticketTestHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, TicketStatus.WAITING);
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isOk());

        streamTestHelper.assertTicketChangedReceivedByPublicStudentAndStaff(streams);
    }

    @Test
    void callNext_withWaitingStudent_doesNotSendToAdmin() throws Exception {
        ticketTestHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, TicketStatus.WAITING);
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isOk());

        assertEquals(TICKET_CHANGED_EVENT, streams.publicStream().awaitEvent(EVENT_WAIT_MILLIS));
        streamTestHelper.assertNoEventReceived(streams.adminStream());
    }

    @Test
    void callNext_withNoWaitingStudent_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isNoContent());
        queueChangeAnnouncer.announceConfigurationChanged();

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void callNext_whenCounterIsAlreadyServing_sendsNothing() throws Exception {
        ticketTestHelper.createCalledTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, deskOne);
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isConflict());
        queueChangeAnnouncer.announceConfigurationChanged();

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void markDone_withCalledTicket_sendsTicketChangedToPublicStudentAndStaff() throws Exception {
        Ticket calledTicket = ticketTestHelper.createCalledTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 1, deskOne);
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.markDone(staffToken, calledTicket.getId()).andExpect(status().isOk());

        streamTestHelper.assertTicketChangedReceivedByPublicStudentAndStaff(streams);
    }

    @Test
    void markDone_withCalledTicket_doesNotSendToAdmin() throws Exception {
        Ticket calledTicket = ticketTestHelper.createCalledTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 1, deskOne);
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.markDone(staffToken, calledTicket.getId()).andExpect(status().isOk());

        assertEquals(TICKET_CHANGED_EVENT, streams.publicStream().awaitEvent(EVENT_WAIT_MILLIS));
        streamTestHelper.assertNoEventReceived(streams.adminStream());
    }

    @Test
    void markDone_withWaitingTicket_sendsNothing() throws Exception {
        Ticket waitingTicket = ticketTestHelper.createTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 1, TicketStatus.WAITING);
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.markDone(staffToken, waitingTicket.getId()).andExpect(status().isConflict());
        queueChangeAnnouncer.announceConfigurationChanged();

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void markNoShow_withCalledTicket_sendsTicketChangedToPublicStudentAndStaff() throws Exception {
        Ticket calledTicket = ticketTestHelper.createCalledTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 1, deskOne);
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.markNoShow(staffToken, calledTicket.getId()).andExpect(status().isOk());

        streamTestHelper.assertTicketChangedReceivedByPublicStudentAndStaff(streams);
    }

    @Test
    void markNoShow_withCalledTicket_doesNotSendToAdmin() throws Exception {
        Ticket calledTicket = ticketTestHelper.createCalledTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 1, deskOne);
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.markNoShow(staffToken, calledTicket.getId()).andExpect(status().isOk());

        assertEquals(TICKET_CHANGED_EVENT, streams.publicStream().awaitEvent(EVENT_WAIT_MILLIS));
        streamTestHelper.assertNoEventReceived(streams.adminStream());
    }

    @Test
    void markNoShow_withWaitingTicket_sendsNothing() throws Exception {
        Ticket waitingTicket = ticketTestHelper.createTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 1, TicketStatus.WAITING);
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.markNoShow(staffToken, waitingTicket.getId()).andExpect(status().isConflict());
        queueChangeAnnouncer.announceConfigurationChanged();

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    private OpenStreams openStreamOfEveryAudience() throws Exception {
        return streamTestHelper.openReadyStreamOfEveryAudience(port, studentToken, staffToken, adminToken);
    }
}
