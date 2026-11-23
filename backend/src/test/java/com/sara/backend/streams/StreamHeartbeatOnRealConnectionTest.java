package com.sara.backend.streams;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.support.OpenStreamConnection;
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

import static com.sara.backend.support.StreamTestHelper.PUBLIC_STREAM_PATH;
import static com.sara.backend.support.StreamTestHelper.TICKET_CHANGED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "dor.streams.heartbeat-interval-millis=200")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Stream heartbeat on a real server")
class StreamHeartbeatOnRealConnectionTest {

    private static final int HEARTBEATS_TO_WAIT_FOR = 3;

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

    private StreamTestHelper streamTestHelper;
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
        TicketTestHelper ticketTestHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        studentToken = ticketTestHelper.registerStudentAndLogin("student@example.com");
        staffToken = ticketTestHelper.loginAsNewStaff();
        adminToken = ticketTestHelper.loginAsAdmin();
    }

    @AfterEach
    void closeOpenStreams() {
        streamTestHelper.close();
    }

    @Test
    void publicStream_acrossSeveralHeartbeats_staysOpenAndReceivesEachHeartbeat() throws Exception {
        OpenStreamConnection connection = streamTestHelper.openReadyStream(port, PUBLIC_STREAM_PATH);

        streamTestHelper.assertHeartbeatsReceived(connection, HEARTBEATS_TO_WAIT_FOR);

        assertFalse(connection.awaitEnd(0), "The stream ended while heartbeats were arriving.");
    }

    @Test
    void allFourStreams_acrossSeveralHeartbeats_stayOpenAndReceiveHeartbeats() throws Exception {
        OpenStreams streams = streamTestHelper.openReadyStreamOfEveryAudience(
                port, studentToken, staffToken, adminToken);

        for (OpenStreamConnection connection : streams.all()) {
            streamTestHelper.assertHeartbeatsReceived(connection, HEARTBEATS_TO_WAIT_FOR);
            assertFalse(connection.awaitEnd(0), "A stream ended while heartbeats were arriving.");
        }
    }

    @Test
    void publicStream_afterHeartbeats_stillReceivesTicketChanged() throws Exception {
        OpenStreamConnection connection = streamTestHelper.openReadyStream(port, PUBLIC_STREAM_PATH);
        streamTestHelper.assertHeartbeatsReceived(connection, HEARTBEATS_TO_WAIT_FOR);

        queueChangeAnnouncer.announceTicketChanged();

        assertEquals(TICKET_CHANGED_EVENT, streamTestHelper.awaitEventSkippingHeartbeats(connection));
    }
}
