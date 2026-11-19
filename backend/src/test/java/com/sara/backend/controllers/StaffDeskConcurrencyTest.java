package com.sara.backend.controllers;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.support.TicketTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffDeskConcurrencyTest {

    private static final int ROUNDS = 20;
    private static final int OK = 200;
    private static final int NO_CONTENT = 204;
    private static final int CONFLICT = 409;

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

    private TicketTestHelper testHelper;
    private ServiceType registration;
    private Counter deskOne;
    private Counter deskTwo;
    private String staffToken;
    private String firstStudentToken;

    @BeforeEach
    void resetDatabaseWithTwoDesksAndTwoStudents() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        registration = testHelper.createActiveService("Registration", "R");
        deskOne = testHelper.createCounter("Desk 1", registration);
        deskTwo = testHelper.createCounter("Desk 2", registration);
        staffToken = testHelper.loginAsNewStaff();
        firstStudentToken = testHelper.registerStudentAndLogin("a@example.com");
        testHelper.registerStudentAndLogin("b@example.com");
    }

    @AfterEach
    void deleteAllTickets() {
        ticketRepository.deleteAll();
    }

    private List<MockHttpServletResponse> runAtTheSameTime(List<Callable<MockHttpServletResponse>> requests)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(requests.size());
        CountDownLatch startSignal = new CountDownLatch(1);
        try {
            List<Future<MockHttpServletResponse>> pendingResponses = new ArrayList<>();
            for (Callable<MockHttpServletResponse> request : requests) {
                pendingResponses.add(executor.submit(() -> {
                    startSignal.await();
                    return request.call();
                }));
            }
            startSignal.countDown();
            List<MockHttpServletResponse> responses = new ArrayList<>();
            for (Future<MockHttpServletResponse> pendingResponse : pendingResponses) {
                responses.add(getOrRethrow(pendingResponse));
            }
            return responses;
        } finally {
            executor.shutdownNow();
        }
    }

    private MockHttpServletResponse getOrRethrow(Future<MockHttpServletResponse> pendingResponse) throws Exception {
        try {
            return pendingResponse.get();
        } catch (ExecutionException failure) {
            throw new AssertionError("A concurrent request failed", failure.getCause());
        }
    }

    private Callable<MockHttpServletResponse> callNextRequest(Counter counter) {
        return () -> testHelper.callNext(staffToken, counter.getId()).andReturn().getResponse();
    }

    private List<Integer> statusesOf(List<MockHttpServletResponse> responses) {
        return responses.stream().map(MockHttpServletResponse::getStatus).sorted().toList();
    }

    private long ticketIdOf(MockHttpServletResponse response) throws Exception {
        Number ticketId = JsonPath.read(response.getContentAsString(), "$.id");
        return ticketId.longValue();
    }

    private Ticket createWaitingTicket(String email, int number) {
        return testHelper.createTicketForRegisteredStudent(email, registration, number, TicketStatus.WAITING);
    }

    @Test
    void callNext_fromTwoCountersAtOnce_returnsTwoDifferentTickets() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            ticketRepository.deleteAll();
            Ticket first = createWaitingTicket("a@example.com", 1);
            Ticket second = createWaitingTicket("b@example.com", 2);

            List<MockHttpServletResponse> responses = runAtTheSameTime(
                    List.of(callNextRequest(deskOne), callNextRequest(deskTwo)));

            assertEquals(List.of(OK, OK), statusesOf(responses));
            Set<Long> calledTicketIds = new HashSet<>();
            for (MockHttpServletResponse response : responses) {
                calledTicketIds.add(ticketIdOf(response));
            }
            assertEquals(Set.of(first.getId(), second.getId()), calledTicketIds);
        }
    }

    @Test
    void callNext_fromTwoCountersAtOnceWithOneWaitingTicket_returnsOneTicketAndOneNoContent() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            ticketRepository.deleteAll();
            Ticket onlyTicket = createWaitingTicket("a@example.com", 1);

            List<MockHttpServletResponse> responses = runAtTheSameTime(
                    List.of(callNextRequest(deskOne), callNextRequest(deskTwo)));

            assertEquals(List.of(OK, NO_CONTENT), statusesOf(responses));
            assertEquals(TicketStatus.CALLED,
                    ticketRepository.findById(onlyTicket.getId()).orElseThrow().getStatus());
        }
    }

    @Test
    void callNext_twiceOnSameCounterAtOnce_returnsOneTicketAndOneConflict() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            ticketRepository.deleteAll();
            createWaitingTicket("a@example.com", 1);
            createWaitingTicket("b@example.com", 2);

            List<MockHttpServletResponse> responses = runAtTheSameTime(
                    List.of(callNextRequest(deskOne), callNextRequest(deskOne)));

            assertEquals(List.of(OK, CONFLICT), statusesOf(responses));
            assertEquals(1, ticketRepository.findAll().stream()
                    .filter(ticket -> ticket.getStatus() == TicketStatus.CALLED).count());
        }
    }

    @Test
    void callNextAndCancel_atOnceOnSameTicket_endWithOneConsistentState() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            ticketRepository.deleteAll();
            Ticket ticket = createWaitingTicket("a@example.com", 1);
            Callable<MockHttpServletResponse> cancelRequest = () -> mockMvc.perform(
                            post("/api/tickets/me/cancel").header("Authorization", "Bearer " + firstStudentToken))
                    .andReturn().getResponse();

            List<MockHttpServletResponse> responses = runAtTheSameTime(
                    List.of(callNextRequest(deskOne), cancelRequest));

            int callNextStatus = responses.get(0).getStatus();
            int cancelStatus = responses.get(1).getStatus();
            TicketStatus finalStatus = ticketRepository.findById(ticket.getId()).orElseThrow().getStatus();
            if (callNextStatus == OK) {
                assertEquals(CONFLICT, cancelStatus);
                assertEquals(TicketStatus.CALLED, finalStatus);
            } else {
                assertEquals(NO_CONTENT, callNextStatus);
                assertEquals(NO_CONTENT, cancelStatus);
                assertEquals(TicketStatus.CANCELLED, finalStatus);
            }
        }
    }

    @Test
    void markDone_twiceAtOnce_returnsOneOkAndOneConflict() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            ticketRepository.deleteAll();
            Ticket called = testHelper.createCalledTicketForRegisteredStudent(
                    "a@example.com", registration, 1, deskOne);
            Callable<MockHttpServletResponse> markDoneRequest =
                    () -> testHelper.markDone(staffToken, called.getId()).andReturn().getResponse();

            List<MockHttpServletResponse> responses = runAtTheSameTime(
                    List.of(markDoneRequest, markDoneRequest));

            assertEquals(List.of(OK, CONFLICT), statusesOf(responses));
            assertEquals(TicketStatus.DONE, ticketRepository.findById(called.getId()).orElseThrow().getStatus());
        }
    }
}
