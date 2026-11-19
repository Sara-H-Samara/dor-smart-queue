package com.sara.backend.controllers;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.configurations.DataSeeder;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Cancelling a ticket")
class CancelTicketControllerTest {

    private static final String MY_TICKET_URL = "/api/tickets/me";
    private static final String CANCEL_MY_TICKET_URL = "/api/tickets/me/cancel";
    private static final String TICKETS_URL = "/api/tickets";

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

    @BeforeEach
    void resetDatabaseWithSeededAdmin() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
    }

    @AfterEach
    void deleteAllTickets() {
        ticketRepository.deleteAll();
    }

    private ResultActions cancelMyTicket(String token) throws Exception {
        return mockMvc.perform(post(CANCEL_MY_TICKET_URL).header("Authorization", "Bearer " + token));
    }

    @Test
    @DisplayName("A student can cancel a waiting ticket")
    void cancelTicket_withWaitingTicket_returnsNoContentAndRemovesActiveTicket() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        String token = testHelper.registerStudentAndLogin("a@example.com");
        Ticket ticket = testHelper.createTicketForRegisteredStudent(
                "a@example.com", registration, 1, TicketStatus.WAITING);

        cancelMyTicket(token).andExpect(status().isNoContent());

        mockMvc.perform(get(MY_TICKET_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        assertEquals(TicketStatus.CANCELLED, ticketRepository.findById(ticket.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("After a cancel, a new ticket gets the next number")
    void cancelTicket_thenTakeTicket_returnsNextNumber() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        String token = testHelper.registerStudentAndLogin("a@example.com");
        Ticket cancelledTicket = testHelper.createTicketForRegisteredStudent(
                "a@example.com", registration, 1, TicketStatus.WAITING);
        cancelMyTicket(token).andExpect(status().isNoContent());

        String newTicketResponse = mockMvc.perform(post(TICKETS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":%d}".formatted(registration.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number newTicketId = JsonPath.read(newTicketResponse, "$.id");
        Ticket newTicket = ticketRepository.findById(newTicketId.longValue()).orElseThrow();
        assertEquals(cancelledTicket.getNumber() + 1, newTicket.getNumber());
        assertEquals(TicketStatus.CANCELLED,
                ticketRepository.findById(cancelledTicket.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("A student cannot cancel a ticket that is already called")
    void cancelTicket_withCalledTicket_returnsConflict() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        String token = testHelper.registerStudentAndLogin("a@example.com");
        Ticket ticket = testHelper.createTicketForRegisteredStudent(
                "a@example.com", registration, 1, TicketStatus.CALLED);

        cancelMyTicket(token).andExpect(status().isConflict());

        assertEquals(TicketStatus.CALLED, ticketRepository.findById(ticket.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("A waiting ticket from yesterday is not cancelled")
    void cancelTicket_withWaitingTicketFromYesterday_returnsNotFoundAndKeepsTicketWaiting() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        String token = testHelper.registerStudentAndLogin("a@example.com");
        Ticket yesterdayTicket = testHelper.createTicketOnDateForRegisteredStudent(
                "a@example.com", registration, 1, LocalDate.now().minusDays(1), TicketStatus.WAITING);

        cancelMyTicket(token).andExpect(status().isNotFound());

        assertEquals(TicketStatus.WAITING, ticketRepository.findById(yesterdayTicket.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("A student without an active ticket has nothing to cancel")
    void cancelTicket_withoutActiveTicket_returnsNotFound() throws Exception {
        String token = testHelper.registerStudentAndLogin("a@example.com");

        cancelMyTicket(token).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Staff cannot cancel a student ticket")
    void cancelTicket_asStaff_returnsForbidden() throws Exception {
        cancelMyTicket(testHelper.loginAsNewStaff()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An admin cannot cancel a student ticket")
    void cancelTicket_asAdmin_returnsForbidden() throws Exception {
        cancelMyTicket(testHelper.loginAsAdmin()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A visitor without a token cannot cancel a ticket")
    void cancelTicket_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(CANCEL_MY_TICKET_URL)).andExpect(status().isUnauthorized());
    }
}
