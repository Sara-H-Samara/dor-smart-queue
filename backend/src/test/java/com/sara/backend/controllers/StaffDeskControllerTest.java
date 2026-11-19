package com.sara.backend.controllers;

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
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("Staff desk")
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
    @DisplayName("Calling next gives the oldest waiting ticket")
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
    @DisplayName("Calling next saves the counter and the call time on the ticket")
    void callNext_withWaitingTickets_recordsCounterAndCalledAt() throws Exception {
        Ticket oldest = waitingTicketFor("a@example.com", 1);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isOk());

        Ticket savedTicket = ticketRepository.findById(oldest.getId()).orElseThrow();
        assertEquals(TicketStatus.CALLED, savedTicket.getStatus());
        assertEquals(deskOne.getId(), savedTicket.getCounter().getId());
        assertNotNull(savedTicket.getCalledAt());
    }

    @Test
    @DisplayName("Calling next with no waiting ticket gives no content")
    void callNext_withNoWaitingTicket_returnsNoContent() throws Exception {
        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("A ticket of another service is not called")
    void callNext_withTicketOfAnotherService_ignoresIt() throws Exception {
        ServiceType payments = testHelper.createActiveService("Payments", "P");
        testHelper.registerStudentAndLogin("a@example.com");
        testHelper.createTicketForRegisteredStudent("a@example.com", payments, 1, TicketStatus.WAITING);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("A ticket from an earlier day is not called")
    void callNext_withTicketFromEarlierDay_ignoresIt() throws Exception {
        testHelper.registerStudentAndLogin("a@example.com");
        testHelper.createTicketOnDateForRegisteredStudent(
                "a@example.com", registration, 1, LocalDate.now().minusDays(1), TicketStatus.WAITING);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("A cancelled ticket is skipped")
    void callNext_withCancelledTicket_skipsIt() throws Exception {
        testHelper.registerStudentAndLogin("a@example.com");
        testHelper.createTicketForRegisteredStudent("a@example.com", registration, 1, TicketStatus.CANCELLED);
        Ticket waiting = waitingTicketFor("b@example.com", 2);

        testHelper.callNext(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(waiting.getId()));
    }

    @Test
    @DisplayName("Calling next at a counter that does not exist gives not found")
    void callNext_withUnknownCounter_returnsNotFound() throws Exception {
        testHelper.callNext(staffToken, UNKNOWN_ID).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("An inactive counter cannot call next")
    void callNext_withInactiveCounter_returnsConflict() throws Exception {
        Ticket waiting = waitingTicketFor("a@example.com", 1);
        deskOne.setActive(false);
        counterRepository.save(deskOne);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.WAITING, statusInDatabase(waiting));
    }

    @Test
    @DisplayName("A counter of an inactive service cannot call next")
    void callNext_withInactiveService_returnsConflict() throws Exception {
        Ticket waiting = waitingTicketFor("a@example.com", 1);
        registration.setActive(false);
        serviceTypeRepository.save(registration);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.WAITING, statusInDatabase(waiting));
    }

    @Test
    @DisplayName("A counter that already serves a student cannot call next")
    void callNext_whenCounterAlreadyHasCalledStudent_returnsConflict() throws Exception {
        calledTicketFor("a@example.com", 1);
        Ticket waiting = waitingTicketFor("b@example.com", 2);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.WAITING, statusInDatabase(waiting));
    }

    @Test
    @DisplayName("A student cannot call next")
    void callNext_asStudent_returnsForbidden() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");

        testHelper.callNext(studentToken, deskOne.getId()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An admin cannot call next")
    void callNext_asAdmin_returnsForbidden() throws Exception {
        testHelper.callNext(testHelper.loginAsAdmin(), deskOne.getId()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A visitor without a token cannot call next")
    void callNext_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/counters/{counterId}/call-next", deskOne.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("The current student of a counter is the student with the called ticket")
    void currentStudent_withCalledTicket_returnsCalledTicket() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);

        testHelper.getCurrentStudent(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(called.getId()))
                .andExpect(jsonPath("$.ticketCode").value("R-001"))
                .andExpect(jsonPath("$.studentName").value("Student"))
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.calledAt").isNotEmpty())
                .andExpect(jsonPath("$.counterName").value("Desk 1"));
    }

    @Test
    @DisplayName("A counter with no called ticket has no current student")
    void currentStudent_withNoCalledTicket_returnsNoContent() throws Exception {
        waitingTicketFor("a@example.com", 1);

        testHelper.getCurrentStudent(staffToken, deskOne.getId()).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("A ticket called at another counter is not the current student")
    void currentStudent_withCalledTicketAtAnotherCounter_returnsNoContent() throws Exception {
        calledTicketFor("a@example.com", 1);
        Counter deskTwo = testHelper.createCounter("Desk 2", registration);

        testHelper.getCurrentStudent(staffToken, deskTwo.getId()).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("After the ticket is marked done, there is no current student")
    void currentStudent_afterMarkDone_returnsNoContent() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);
        testHelper.markDone(staffToken, called.getId()).andExpect(status().isOk());

        testHelper.getCurrentStudent(staffToken, deskOne.getId()).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("After the ticket is marked as no show, there is no current student")
    void currentStudent_afterMarkNoShow_returnsNoContent() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);
        testHelper.markNoShow(staffToken, called.getId()).andExpect(status().isOk());

        testHelper.getCurrentStudent(staffToken, deskOne.getId()).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("A ticket called on an earlier day is still the current student")
    void currentStudent_withCalledTicketFromEarlierDay_returnsCalledTicket() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);
        called.setTicketDate(LocalDate.now().minusDays(1));
        ticketRepository.save(called);

        testHelper.getCurrentStudent(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(called.getId()));
    }

    @Test
    @DisplayName("An inactive counter still shows its current student")
    void currentStudent_withInactiveCounter_returnsCalledTicket() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);
        deskOne.setActive(false);
        counterRepository.save(deskOne);

        testHelper.getCurrentStudent(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(called.getId()));
    }

    @Test
    @DisplayName("The current student of a counter that does not exist gives not found")
    void currentStudent_withUnknownCounter_returnsNotFound() throws Exception {
        testHelper.getCurrentStudent(staffToken, UNKNOWN_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Counter not found."));
    }

    @Test
    @DisplayName("A student cannot see the current student of a counter")
    void currentStudent_asStudent_returnsForbidden() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");

        testHelper.getCurrentStudent(studentToken, deskOne.getId()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An admin cannot see the current student of a counter")
    void currentStudent_asAdmin_returnsForbidden() throws Exception {
        testHelper.getCurrentStudent(testHelper.loginAsAdmin(), deskOne.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A visitor without a token cannot see the current student")
    void currentStudent_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/counters/{counterId}/current", deskOne.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A called ticket can be marked done")
    void markDone_withCalledTicket_returnsDone() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);

        testHelper.markDone(staffToken, called.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        assertEquals(TicketStatus.DONE, statusInDatabase(called));
    }

    @Test
    @DisplayName("A waiting ticket cannot be marked done")
    void markDone_withWaitingTicket_returnsConflict() throws Exception {
        Ticket waiting = waitingTicketFor("a@example.com", 1);

        testHelper.markDone(staffToken, waiting.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.WAITING, statusInDatabase(waiting));
    }

    @Test
    @DisplayName("A done ticket cannot be marked done again")
    void markDone_withDoneTicket_returnsConflict() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);
        testHelper.markDone(staffToken, called.getId()).andExpect(status().isOk());

        testHelper.markDone(staffToken, called.getId()).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A cancelled ticket cannot be marked done")
    void markDone_withCancelledTicket_returnsConflict() throws Exception {
        testHelper.registerStudentAndLogin("a@example.com");
        Ticket cancelled = testHelper.createTicketForRegisteredStudent(
                "a@example.com", registration, 1, TicketStatus.CANCELLED);

        testHelper.markDone(staffToken, cancelled.getId()).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A ticket that does not exist cannot be marked done")
    void markDone_withUnknownTicket_returnsNotFound() throws Exception {
        testHelper.markDone(staffToken, UNKNOWN_ID).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A called ticket can be marked as no show")
    void markNoShow_withCalledTicket_returnsNoShow() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);

        testHelper.markNoShow(staffToken, called.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        assertEquals(TicketStatus.NO_SHOW, statusInDatabase(called));
    }

    @Test
    @DisplayName("A waiting ticket cannot be marked as no show")
    void markNoShow_withWaitingTicket_returnsConflict() throws Exception {
        Ticket waiting = waitingTicketFor("a@example.com", 1);

        testHelper.markNoShow(staffToken, waiting.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.WAITING, statusInDatabase(waiting));
    }

    @Test
    @DisplayName("A done ticket cannot be marked as no show")
    void markNoShow_withDoneTicket_returnsConflict() throws Exception {
        Ticket called = calledTicketFor("a@example.com", 1);
        testHelper.markDone(staffToken, called.getId()).andExpect(status().isOk());

        testHelper.markNoShow(staffToken, called.getId()).andExpect(status().isConflict());

        assertEquals(TicketStatus.DONE, statusInDatabase(called));
    }

    @Test
    @DisplayName("A ticket that does not exist cannot be marked as no show")
    void markNoShow_withUnknownTicket_returnsNotFound() throws Exception {
        testHelper.markNoShow(staffToken, UNKNOWN_ID).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A student cannot mark a ticket done")
    void markDone_asStudent_returnsForbidden() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");
        Ticket called = testHelper.createCalledTicketForRegisteredStudent(
                "a@example.com", registration, 1, deskOne);

        testHelper.markDone(studentToken, called.getId()).andExpect(status().isForbidden());

        assertEquals(TicketStatus.CALLED, statusInDatabase(called));
    }

    @Test
    @DisplayName("A visitor without a token cannot mark a ticket done")
    void markDone_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/tickets/{ticketId}/done", UNKNOWN_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A student cannot mark a ticket as no show")
    void markNoShow_asStudent_returnsForbidden() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");
        Ticket called = testHelper.createCalledTicketForRegisteredStudent(
                "a@example.com", registration, 1, deskOne);

        testHelper.markNoShow(studentToken, called.getId()).andExpect(status().isForbidden());

        assertEquals(TicketStatus.CALLED, statusInDatabase(called));
    }

    @Test
    @DisplayName("A visitor without a token cannot mark a ticket as no show")
    void markNoShow_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/tickets/{ticketId}/no-show", UNKNOWN_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A student with a called ticket sees the counter name")
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
    @DisplayName("A student with a waiting ticket sees no counter name")
    void getMyTicket_whenWaiting_returnsNoCounterName() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");
        testHelper.createTicketForRegisteredStudent("a@example.com", registration, 1, TicketStatus.WAITING);

        mockMvc.perform(get(MY_TICKET_URL).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterName").isEmpty());
    }
}
