package com.sara.backend.streams;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.support.OpenStreamConnection;
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

import static com.sara.backend.support.StreamTestHelper.ADMIN_STREAM_PATH;
import static com.sara.backend.support.StreamTestHelper.EVENT_WAIT_MILLIS;
import static com.sara.backend.support.StreamTestHelper.PUBLIC_STREAM_PATH;
import static com.sara.backend.support.StreamTestHelper.READY_EVENT;
import static com.sara.backend.support.StreamTestHelper.STAFF_STREAM_PATH;
import static com.sara.backend.support.StreamTestHelper.STUDENT_STREAM_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "dor.streams.lifetime-millis=1000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Stream lifetime on a real server")
class StreamLifetimeTest {

    private static final long STREAM_END_WAIT_MILLIS = 8000;

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
    void publicStream_afterLifetime_endsCleanly() throws Exception {
        OpenStreamConnection connection = streamTestHelper.openStream(port, PUBLIC_STREAM_PATH);

        assertStreamEndsCleanly(connection);
    }

    @Test
    void studentStream_afterLifetime_endsCleanly() throws Exception {
        OpenStreamConnection connection = streamTestHelper.openStream(port, STUDENT_STREAM_PATH, studentToken);

        assertStreamEndsCleanly(connection);
    }

    @Test
    void staffStream_afterLifetime_endsCleanly() throws Exception {
        OpenStreamConnection connection = streamTestHelper.openStream(port, STAFF_STREAM_PATH, staffToken);

        assertStreamEndsCleanly(connection);
    }

    @Test
    void adminStream_afterLifetime_endsCleanly() throws Exception {
        OpenStreamConnection connection = streamTestHelper.openStream(port, ADMIN_STREAM_PATH, adminToken);

        assertStreamEndsCleanly(connection);
    }

    @Test
    void streams_afterLifetime_areRemovedFromRegistry() throws Exception {
        streamTestHelper.openStream(port, PUBLIC_STREAM_PATH);
        streamTestHelper.openStream(port, STUDENT_STREAM_PATH, studentToken);
        streamTestHelper.openStream(port, STAFF_STREAM_PATH, staffToken);
        streamTestHelper.openStream(port, ADMIN_STREAM_PATH, adminToken);

        assertTrue(streamTestHelper.awaitNoRegisteredEmitters(STREAM_END_WAIT_MILLIS));
    }

    @Test
    void studentStream_afterEarlierStreamEnded_opensNewConnectionWithReadyEvent() throws Exception {
        OpenStreamConnection firstConnection = streamTestHelper.openStream(port, STUDENT_STREAM_PATH, studentToken);
        assertStreamEndsCleanly(firstConnection);

        OpenStreamConnection secondConnection = streamTestHelper.openStream(port, STUDENT_STREAM_PATH, studentToken);

        assertEquals(200, secondConnection.statusCode());
        assertEquals(READY_EVENT, secondConnection.awaitEvent(EVENT_WAIT_MILLIS));
    }

    private void assertStreamEndsCleanly(OpenStreamConnection connection) {
        assertEquals(200, connection.statusCode());
        assertEquals(READY_EVENT, connection.awaitEvent(EVENT_WAIT_MILLIS));
        assertTrue(connection.awaitEnd(STREAM_END_WAIT_MILLIS), "The stream did not end after its lifetime.");
        assertTrue(connection.endedCleanly(), "The stream ended with an error instead of a clean end.");
    }
}
