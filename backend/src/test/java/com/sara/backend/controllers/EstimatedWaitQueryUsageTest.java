package com.sara.backend.controllers;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.services.TicketService;
import com.sara.backend.support.TicketTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Estimated wait query usage")
class EstimatedWaitQueryUsageTest {

    private static final String STUDENT_EMAIL = "a@example.com";
    private static final String REPOSITORY_FIELD_OF_TICKET_SERVICE = "ticketRepository";

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
    private TicketService ticketService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataSeeder dataSeeder;

    private TicketTestHelper testHelper;
    private Counter window;
    private String studentToken;
    private Object ticketServiceTarget;
    private TicketRepository countingTicketRepository;

    @BeforeEach
    void resetDatabaseAndCountCallsOfTicketService() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        window = testHelper.createCounter("Window 1", registration);
        studentToken = testHelper.registerStudentAndLogin(STUDENT_EMAIL);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, TicketStatus.WAITING);

        ticketServiceTarget = AopTestUtils.getUltimateTargetObject(ticketService);
        countingTicketRepository = Mockito.mock(
                TicketRepository.class, AdditionalAnswers.delegatesTo(ticketRepository));
        ReflectionTestUtils.setField(
                ticketServiceTarget, REPOSITORY_FIELD_OF_TICKET_SERVICE, countingTicketRepository);
    }

    @AfterEach
    void restoreRepositoryAndDeleteAllTickets() {
        ReflectionTestUtils.setField(ticketServiceTarget, REPOSITORY_FIELD_OF_TICKET_SERVICE, ticketRepository);
        ticketRepository.deleteAll();
    }

    @Test
    void getActiveTicket_withOpenCounter_loadsFinishedTicketsOnce() throws Exception {
        testHelper.getMyActiveTicket(studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));

        verify(countingTicketRepository, times(1)).findRecentFinishedTicketRowsOfService(any(), any(), any());
    }

    @Test
    void getActiveTicket_withoutOpenCounter_doesNotLoadFinishedTickets() throws Exception {
        window.setActive(false);
        counterRepository.save(window);

        testHelper.getMyActiveTicket(studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasOpenCounter").value(false))
                .andExpect(jsonPath("$.estimatedWaitMinutes").isEmpty());

        verify(countingTicketRepository, never()).findRecentFinishedTicketRowsOfService(any(), any(), any());
    }
}
