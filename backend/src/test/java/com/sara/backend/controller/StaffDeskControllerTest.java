package com.sara.backend.controller;

import com.sara.backend.config.DataSeeder;
import com.sara.backend.entity.Counter;
import com.sara.backend.entity.ServiceType;
import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;
import com.sara.backend.repository.CounterRepository;
import com.sara.backend.repository.ServiceTypeRepository;
import com.sara.backend.repository.TicketRepository;
import com.sara.backend.repository.UserRepository;
import com.sara.backend.support.TicketTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffDeskControllerTest {

    private static final String MY_TICKET_URL = "/api/tickets/me";
    private static final long UNKNOWN_ID = 999999L;

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
    private String staffToken;

    @BeforeEach
    void resetDatabaseWithStaffAndCounter() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        registration = testHelper.createActiveService("Registration", "R");
        deskOne = testHelper.createCounter("Desk 1", registration);
        staffToken = testHelper.loginAsNewStaff();
    }

    @AfterEach
    void deleteAllTickets() {
        ticketRepository.deleteAll();
    }

    private Ticket waitingTicketFor(String email, int number) throws Exception {
        testHelper.registerStudentAndLogin(email);
        return testHelper.createTicketForRegisteredStudent(email, registration, number, TicketStatus.WAITING);
    }

    private Ticket calledTicketFor(String email, int number) throws Exception {
        testHelper.registerStudentAndLogin(email);
        return testHelper.createCalledTicketForRegisteredStudent(email, registration, number, deskOne);
    }

    private TicketStatus statusInDatabase(Ticket ticket) {
        return ticketRepository.findById(ticket.getId()).orElseThrow().getStatus();
    }

    @Test
    void callNext_withWaitingTickets_returnsOldestWaitingTicket() throws Exception {
        Ticket oldest = waitingTicketFor("a@example.com", 1);
        waitingTicketFor("b@example.com", 2);

        testHelper.callNext(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(oldest.getId()))
                .andExpect(jsonPath("$.ticketCode").value("R-001"))
                .andExpect(jsonPath("$.studentName").value("Student"))
                .andExpect(jsonPath("$.status").value("CALLED"));
    }

    @Test
    void callNext_withWaitingTickets_recordsCounterAndCalledAt() throws Exception {
        Ticket oldest = waitingTicketFor("a@example.com", 1);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isOk());

        Ticket savedTicket = ticketRepository.findById(oldest.getId()).orElseThrow();
        assertEquals(TicketStatus.CALLED, savedTicket.getStatus());
        assertEquals(deskOne.getId(), savedTicket.getCounter().getId());
        assertNotNull(savedTicket.getCalledAt());
    }

    @Test
    void callNext_withNoWaitingTicket_returnsNoContent() throws Exception {
        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isNoContent());
    }

    @Test
    void callNext_withTicketOfAnotherService_ignoresIt() throws Exception {
        ServiceType payments = testHelper.createActiveService("Payments", "P");
        testHelper.registerStudentAndLogin("a@example.com");
        testHelper.createTicketForRegisteredStudent("a@example.com", payments, 1, TicketStatus.WAITING);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isNoContent());
    }

    @Test
    void callNext_withTicketFromEarlierDay_ignoresIt() throws Exception {
        testHelper.registerStudentAndLogin("a@example.com");
        testHelper.createTicketOnDateForRegisteredStudent(
                "a@example.com", registration, 1, LocalDate.now().minusDays(1), TicketStatus.WAITING);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isNoContent());
    }

    @Test
    void callNext_withCancelledTicket_skipsIt() throws Exception {
        testHelper.registerStudentAndLogin("a@example.com");
        testHelper.createTicketForRegisteredStudent("a@example.com", registration, 1, TicketStatus.CANCELLED);
        Ticket waiting = waitingTicketFor("b@example.com", 2);

        testHelper.callNext(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(waiting.getId()));
    }

    @Test
    void callNext_withUnknownCounter_returnsNotFound() throws Exception {
        testHelper.callNext(staffToken, UNKNOWN_ID).andExpect(status().isNotFound());
    }

    @Test
    void callNext_withInactiveCounter_returnsConflict() throws Exception {
        Ticket waiting = waitingTicketFor("a@example.com", 1);
        deskOne.setActive(false);
        counterRepository.save(deskOne);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.WAITING, statusInDatabase(waiting));
    }

    @Test
    void callNext_withInactiveService_returnsConflict() throws Exception {
        Ticket waiting = waitingTicketFor("a@example.com", 1);
        registration.setActive(false);
        serviceTypeRepository.save(registration);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.WAITING, statusInDatabase(waiting));
    }

    @Test
    void callNext_whenCounterAlreadyHasCalledStudent_returnsConflict() throws Exception {
        calledTicketFor("a@example.com", 1);
        Ticket waiting = waitingTicketFor("b@example.com", 2);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.WAITING, statusInDatabase(waiting));
    }

    @Test
    void callNext_asStudent_returnsForbidden() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");

        testHelper.callNext(studentToken, deskOne.getId()).andExpect(status().isForbidden());
    }

    @Test
    void callNext_asAdmin_returnsForbidden() throws Exception {
        testHelper.callNext(testHelper.loginAsAdmin(), deskOne.getId()).andExpect(status().isForbidden());
    }

    @Test
    void callNext_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/counters/{counterId}/call-next", deskOne.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markDone_withCalledTicket_returnsDone() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);

        testHelper.markDone(staffToken, called.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        assertEquals(TicketStatus.DONE, statusInDatabase(called));
    }

    @Test
    void markDone_withWaitingTicket_returnsConflict() throws Exception {
        Ticket waiting = waitingTicketFor("a@example.com", 1);

        testHelper.markDone(staffToken, waiting.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.WAITING, statusInDatabase(waiting));
    }

    @Test
    void markDone_withDoneTicket_returnsConflict() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);
        testHelper.markDone(staffToken, called.getId()).andExpect(status().isOk());

        testHelper.markDone(staffToken, called.getId()).andExpect(status().isConflict());
    }

    @Test
    void markDone_withCancelledTicket_returnsConflict() throws Exception {
        testHelper.registerStudentAndLogin("a@example.com");
        Ticket cancelled = testHelper.createTicketForRegisteredStudent(
                "a@example.com", registration, 1, TicketStatus.CANCELLED);

        testHelper.markDone(staffToken, cancelled.getId()).andExpect(status().isConflict());
    }

    @Test
    void markDone_withUnknownTicket_returnsNotFound() throws Exception {
        testHelper.markDone(staffToken, UNKNOWN_ID).andExpect(status().isNotFound());
    }

    @Test
    void markNoShow_withCalledTicket_returnsNoShow() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);

        testHelper.markNoShow(staffToken, called.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        assertEquals(TicketStatus.NO_SHOW, statusInDatabase(called));
    }

    @Test
    void markNoShow_withWaitingTicket_returnsConflict() throws Exception {
        Ticket waiting = waitingTicketFor("a@example.com", 1);

        testHelper.markNoShow(staffToken, waiting.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.WAITING, statusInDatabase(waiting));
    }

    @Test
    void markNoShow_withDoneTicket_returnsConflict() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);
        testHelper.markDone(staffToken, called.getId()).andExpect(status().isOk());

        testHelper.markNoShow(staffToken, called.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.DONE, statusInDatabase(called));
    }

    @Test
    void markNoShow_withUnknownTicket_returnsNotFound() throws Exception {
        testHelper.markNoShow(staffToken, UNKNOWN_ID).andExpect(status().isNotFound());
    }

    @Test
    void markDone_asStudent_returnsForbidden() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");
        Ticket called = testHelper.createCalledTicketForRegisteredStudent(
                "a@example.com", registration, 1, deskOne);

        testHelper.markDone(studentToken, called.getId()).andExpect(status().isForbidden());

        assertEquals(TicketStatus.CALLED, statusInDatabase(called));
    }

    @Test
    void markDone_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/tickets/{ticketId}/done", UNKNOWN_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markNoShow_asStudent_returnsForbidden() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");
        Ticket called = testHelper.createCalledTicketForRegisteredStudent(
                "a@example.com", registration, 1, deskOne);

        testHelper.markNoShow(studentToken, called.getId()).andExpect(status().isForbidden());

        assertEquals(TicketStatus.CALLED, statusInDatabase(called));
    }

    @Test
    void markNoShow_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/tickets/{ticketId}/no-show", UNKNOWN_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyTicket_whenCalled_returnsCounterName() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");
        testHelper.createTicketForRegisteredStudent("a@example.com", registration, 1, TicketStatus.WAITING);
        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isOk());

        mockMvc.perform(get(MY_TICKET_URL).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.counterName").value("Desk 1"));
    }

    @Test
    void getMyTicket_whenWaiting_returnsNoCounterName() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");
        testHelper.createTicketForRegisteredStudent("a@example.com", registration, 1, TicketStatus.WAITING);

        mockMvc.perform(get(MY_TICKET_URL).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterName").isEmpty());
    }
}
