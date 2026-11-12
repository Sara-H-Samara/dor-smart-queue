package com.sara.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.config.DataSeeder;
import com.sara.backend.entity.ServiceType;
import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CancelTicketControllerTest {

    private static final String MY_TICKET_URL = "/api/tickets/me";
    private static final String CANCEL_MY_TICKET_URL = "/api/tickets/me/cancel";
    private static final String TICKETS_URL = "/api/tickets";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, passwordEncoder);
    }

    @AfterEach
    void deleteAllTickets() {
        ticketRepository.deleteAll();
    }

    private ResultActions cancelMyTicket(String token) throws Exception {
        return mockMvc.perform(post(CANCEL_MY_TICKET_URL).header("Authorization", "Bearer " + token));
    }

    @Test
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
    void cancelTicket_thenTakeTicket_returnsNextNumber() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
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
    void cancelTicket_withCalledTicket_returnsConflict() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        String token = testHelper.registerStudentAndLogin("a@example.com");
        Ticket ticket = testHelper.createTicketForRegisteredStudent(
                "a@example.com", registration, 1, TicketStatus.CALLED);

        cancelMyTicket(token).andExpect(status().isConflict());

        assertEquals(TicketStatus.CALLED, ticketRepository.findById(ticket.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelTicket_withoutActiveTicket_returnsNotFound() throws Exception {
        String token = testHelper.registerStudentAndLogin("a@example.com");

        cancelMyTicket(token).andExpect(status().isNotFound());
    }

    @Test
    void cancelTicket_asStaff_returnsForbidden() throws Exception {
        cancelMyTicket(testHelper.loginAsNewStaff()).andExpect(status().isForbidden());
    }

    @Test
    void cancelTicket_asAdmin_returnsForbidden() throws Exception {
        cancelMyTicket(testHelper.loginAsAdmin()).andExpect(status().isForbidden());
    }

    @Test
    void cancelTicket_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(CANCEL_MY_TICKET_URL)).andExpect(status().isUnauthorized());
    }
}
