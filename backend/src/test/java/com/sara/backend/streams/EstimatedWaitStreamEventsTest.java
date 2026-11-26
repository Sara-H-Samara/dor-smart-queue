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

import java.time.Duration;
import java.time.LocalDateTime;

import static com.sara.backend.support.StreamTestHelper.STUDENT_STREAM_PATH;
import static com.sara.backend.support.StreamTestHelper.TICKET_CHANGED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Estimated wait on a real server")
class EstimatedWaitStreamEventsTest {

    private static final String FINISHED_STUDENT_EMAIL = "finished@example.com";
    private static final String CALLED_STUDENT_EMAIL = "called@example.com";
    private static final String WATCHING_STUDENT_EMAIL = "watching@example.com";

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

    private TicketTestHelper ticketTestHelper;
    private StreamTestHelper streamTestHelper;
    private Counter deskOne;
    private Ticket calledStudentTicket;
    private String watchingStudentToken;
    private String staffToken;

    @BeforeEach
    void resetDatabaseAndPrepareTwoUsableFinishedTickets() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        streamTestHelper = new StreamTestHelper(queueStreamRegistry);
        streamTestHelper.removeAllEmitters();
        ticketTestHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        ServiceType registration = ticketTestHelper.createActiveService("Registration", "R");
        deskOne = ticketTestHelper.createCounter("Desk 1", registration);
        ticketTestHelper.registerStudentAndLogin(FINISHED_STUDENT_EMAIL);
        ticketTestHelper.registerStudentAndLogin(CALLED_STUDENT_EMAIL);
        watchingStudentToken = ticketTestHelper.registerStudentAndLogin(WATCHING_STUDENT_EMAIL);
        staffToken = ticketTestHelper.loginAsNewStaff();
        ticketTestHelper.createFinishedTicketForRegisteredStudent(
                FINISHED_STUDENT_EMAIL, registration, 1, deskOne, Duration.ofMinutes(10));
        ticketTestHelper.createFinishedTicketForRegisteredStudent(
                FINISHED_STUDENT_EMAIL, registration, 2, deskOne, Duration.ofMinutes(10));
        ticketTestHelper.createFinishedTicketForRegisteredStudent(
                FINISHED_STUDENT_EMAIL, registration, 3, deskOne, Duration.ofSeconds(10));
        calledStudentTicket = ticketTestHelper.createTicketForRegisteredStudent(
                CALLED_STUDENT_EMAIL, registration, 4, TicketStatus.WAITING);
        ticketTestHelper.createTicketForRegisteredStudent(
                WATCHING_STUDENT_EMAIL, registration, 5, TicketStatus.WAITING);
    }

    @AfterEach
    void closeOpenStreams() {
        streamTestHelper.close();
    }

    @Test
    void markDone_whenItGivesThirdUsableServiceTime_sendsTicketChangedAndNextEstimateChanges() throws Exception {
        OpenStreamConnection studentStream = streamTestHelper.openReadyStream(
                port, STUDENT_STREAM_PATH, watchingStudentToken);
        ticketTestHelper.getMyActiveTicket(watchingStudentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(10));

        ticketTestHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isOk());

        assertEquals(TICKET_CHANGED_EVENT, streamTestHelper.awaitEventSkippingHeartbeats(studentStream));
        ticketTestHelper.getMyActiveTicket(watchingStudentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));

        Ticket calledTicket = ticketRepository.findById(calledStudentTicket.getId()).orElseThrow();
        calledTicket.setCalledAt(LocalDateTime.now().minusMinutes(20));
        ticketRepository.save(calledTicket);
        ticketTestHelper.markDone(staffToken, calledStudentTicket.getId()).andExpect(status().isOk());

        assertEquals(TICKET_CHANGED_EVENT, streamTestHelper.awaitEventSkippingHeartbeats(studentStream));
        ticketTestHelper.getMyActiveTicket(watchingStudentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(14));
    }
}
