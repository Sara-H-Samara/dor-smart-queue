package com.sara.backend.streams;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.support.OpenStreamConnection;
import com.sara.backend.support.StreamTestHelper;
import com.sara.backend.support.TicketTestHelper;
import com.zaxxer.hikari.HikariDataSource;
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

import javax.sql.DataSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Ticket changes on a real server")
class TicketStreamEventsTest {

    private static final String PUBLIC_STREAM_PATH = "/api/public/queue-stream";
    private static final String STUDENT_STREAM_PATH = "/api/tickets/me/stream";
    private static final String STAFF_STREAM_PATH = "/api/staff/stream";
    private static final String ADMIN_STREAM_PATH = "/api/admin/stream";
    private static final String READY_EVENT = "event:ready\ndata:{}";
    private static final String TICKET_CHANGED_EVENT = "event:ticket-changed\ndata:{}";
    private static final String CONFIGURATION_CHANGED_EVENT = "event:configuration-changed\ndata:{}";
    private static final long EVENT_WAIT_MILLIS = 5000;
    private static final long NO_EVENT_WAIT_MILLIS = 300;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int STREAMS_BEYOND_POOL_SIZE = 5;

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

    @Autowired
    private DataSource dataSource;

    private TicketTestHelper ticketTestHelper;
    private StreamTestHelper streamTestHelper;
    private ServiceType registration;
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
        ticketTestHelper.createCounter("Desk 1", registration);
        studentToken = ticketTestHelper.registerStudentAndLogin("student@example.com");
        staffToken = ticketTestHelper.loginAsNewStaff();
        adminToken = ticketTestHelper.loginAsAdmin();
    }

    @AfterEach
    void closeOpenStreams() {
        streamTestHelper.closeAllStreams();
    }

    @Test
    void takeTicket_withOpenStreams_sendsTicketChangedToPublicStudentAndStaff() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.takeTicket(studentToken, registration.getId()).andExpect(status().isCreated());

        assertEquals(TICKET_CHANGED_EVENT, streams.publicStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertEquals(TICKET_CHANGED_EVENT, streams.studentStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertEquals(TICKET_CHANGED_EVENT, streams.staffStream().awaitEvent(EVENT_WAIT_MILLIS));
    }

    @Test
    void takeTicket_withOpenAdminStream_doesNotSendToAdmin() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.takeTicket(studentToken, registration.getId()).andExpect(status().isCreated());

        assertEquals(TICKET_CHANGED_EVENT, streams.publicStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertNull(streams.adminStream().awaitEvent(NO_EVENT_WAIT_MILLIS));
    }

    @Test
    void cancelMyTicket_withOpenStreams_sendsTicketChangedToPublicStudentAndStaff() throws Exception {
        ticketTestHelper.takeTicket(studentToken, registration.getId()).andExpect(status().isCreated());
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.cancelMyTicket(studentToken).andExpect(status().isNoContent());

        assertEquals(TICKET_CHANGED_EVENT, streams.publicStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertEquals(TICKET_CHANGED_EVENT, streams.studentStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertEquals(TICKET_CHANGED_EVENT, streams.staffStream().awaitEvent(EVENT_WAIT_MILLIS));
    }

    @Test
    void cancelMyTicket_withOpenAdminStream_doesNotSendToAdmin() throws Exception {
        ticketTestHelper.takeTicket(studentToken, registration.getId()).andExpect(status().isCreated());
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.cancelMyTicket(studentToken).andExpect(status().isNoContent());

        assertEquals(TICKET_CHANGED_EVENT, streams.publicStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertNull(streams.adminStream().awaitEvent(NO_EVENT_WAIT_MILLIS));
    }

    @Test
    void takeTicket_whenStudentAlreadyHasActiveTicket_sendsNothing() throws Exception {
        ticketTestHelper.takeTicket(studentToken, registration.getId()).andExpect(status().isCreated());
        OpenStreams streams = openStreamOfEveryAudience();

        ticketTestHelper.takeTicket(studentToken, registration.getId()).andExpect(status().isConflict());
        queueChangeAnnouncer.announceConfigurationChanged();

        assertEquals(CONFIGURATION_CHANGED_EVENT, streams.publicStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertNull(streams.publicStream().awaitEvent(NO_EVENT_WAIT_MILLIS));
        assertEquals(CONFIGURATION_CHANGED_EVENT, streams.studentStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertNull(streams.studentStream().awaitEvent(NO_EVENT_WAIT_MILLIS));
        assertEquals(CONFIGURATION_CHANGED_EVENT, streams.staffStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertNull(streams.staffStream().awaitEvent(NO_EVENT_WAIT_MILLIS));
    }

    @Test
    void takeTicket_withMoreOpenStreamsThanDatabasePool_stillServesOtherRequestsAndSendsToAll() throws Exception {
        int streamCount = databasePoolSize() + STREAMS_BEYOND_POOL_SIZE;
        List<OpenStreamConnection> openStreams = new ArrayList<>();
        for (int streamNumber = 0; streamNumber < streamCount; streamNumber++) {
            openStreams.add(openAndConfirmReady(PUBLIC_STREAM_PATH, null));
            openStreams.add(openAndConfirmReady(STUDENT_STREAM_PATH, studentToken));
        }

        assertEquals(200, streamTestHelper.fetchStatusCode(port, "/api/public/queue-status", REQUEST_TIMEOUT));
        ticketTestHelper.takeTicket(studentToken, registration.getId()).andExpect(status().isCreated());

        for (OpenStreamConnection openStream : openStreams) {
            assertEquals(TICKET_CHANGED_EVENT, openStream.awaitEvent(EVENT_WAIT_MILLIS));
        }
    }

    private int databasePoolSize() throws Exception {
        return dataSource.unwrap(HikariDataSource.class).getMaximumPoolSize();
    }

    private OpenStreams openStreamOfEveryAudience() throws Exception {
        return new OpenStreams(
                openAndConfirmReady(PUBLIC_STREAM_PATH, null),
                openAndConfirmReady(STUDENT_STREAM_PATH, studentToken),
                openAndConfirmReady(STAFF_STREAM_PATH, staffToken),
                openAndConfirmReady(ADMIN_STREAM_PATH, adminToken));
    }

    private OpenStreamConnection openAndConfirmReady(String path, String token) throws Exception {
        OpenStreamConnection connection = token == null
                ? streamTestHelper.openStream(port, path)
                : streamTestHelper.openStream(port, path, token);
        assertEquals(200, connection.statusCode());
        assertEquals(READY_EVENT, connection.awaitEvent(EVENT_WAIT_MILLIS));
        return connection;
    }

    private record OpenStreams(
            OpenStreamConnection publicStream,
            OpenStreamConnection studentStream,
            OpenStreamConnection staffStream,
            OpenStreamConnection adminStream) {
    }
}
