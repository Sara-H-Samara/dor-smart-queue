package com.sara.backend.controllers;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.Role;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.entities.User;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyTicketControllerTest {

    private static final String URL = "/api/tickets/me";

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
    void deleteAllTestData() {
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
        // Other test classes do not clear tickets, so leave none behind.
        ticketRepository.deleteAll();
    }

    // ---------- Helpers ----------

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String staffToken() throws Exception {
        User staff = new User();
        staff.setFullName("Test Staff");
        staff.setEmail("test-staff@example.com");
        staff.setPasswordHash(passwordEncoder.encode("staff12345"));
        staff.setRole(Role.STAFF);
        userRepository.save(staff);
        return loginAndGetToken("test-staff@example.com", "staff12345");
    }

    private String registerStudentAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Student","email":"%s","password":"secret123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        return loginAndGetToken(email, "secret123");
    }

    private ServiceType createService(String name, String code) {
        ServiceType service = new ServiceType();
        service.setName(name);
        service.setCode(code);
        service.setActive(true);
        return serviceTypeRepository.save(service);
    }

    /** Registers a student and gives them a ticket directly through the repository. */
    private Ticket ticketFor(String email, ServiceType service, int number, LocalDate date, TicketStatus status)
            throws Exception {
        registerStudentAndLogin(email);
        Ticket ticket = new Ticket();
        ticket.setStudent(userRepository.findByEmail(email).orElseThrow());
        ticket.setServiceType(service);
        ticket.setNumber(number);
        ticket.setTicketDate(date);
        ticket.setStatus(status);
        return ticketRepository.save(ticket);
    }

    private ResultActions me(String email) throws Exception {
        return mockMvc.perform(get(URL).header("Authorization", "Bearer " + loginAndGetToken(email, "secret123")));
    }

    // ---------- Tests ----------

    @Test
    void noActiveTicket_returnsNotFound() throws Exception {
        registerStudentAndLogin("a@example.com");

        me("a@example.com")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("You do not have an active ticket."));
    }

    @Test
    void firstInLine_hasPositionOne() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("R-001"))
                .andExpect(jsonPath("$.serviceName").value("Registration"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.peopleAhead").value(0))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));
    }

    @Test
    void thirdInLine_hasPositionThree() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.WAITING);
        ticketFor("b@example.com", registration, 2, LocalDate.now(), TicketStatus.WAITING);
        ticketFor("c@example.com", registration, 3, LocalDate.now(), TicketStatus.WAITING);

        me("c@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("R-003"))
                .andExpect(jsonPath("$.position").value(3))
                .andExpect(jsonPath("$.peopleAhead").value(2))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(15));
    }

    @Test
    void calledTicket_hasNoPositionOrEstimate() throws Exception {
        ServiceType registration = createService("Registration", "R");
        ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.CALLED);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.position").isEmpty())
                .andExpect(jsonPath("$.peopleAhead").isEmpty())
                .andExpect(jsonPath("$.estimatedWaitMinutes").isEmpty());
    }

    @Test
    void getActiveTicket_withTicketInOtherService_doesNotCountItAhead() throws Exception {
        ServiceType registration = createService("Registration", "R");
        ServiceType documents = createService("Documents", "D");
        testHelper.createCounter("Window 1", registration);
        ticketFor("other@example.com", documents, 1, LocalDate.now(), TicketStatus.WAITING);
        ticketFor("a@example.com", registration, 2, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.peopleAhead").value(0))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));
    }

    @Test
    void getActiveTicket_withTicketFromEarlierDay_doesNotCountItAhead() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        ticketFor("old@example.com", registration, 1, LocalDate.now().minusDays(1), TicketStatus.WAITING);
        ticketFor("a@example.com", registration, 2, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.peopleAhead").value(0))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));
    }

    @Test
    void getActiveTicket_withOnlyWaitingTicketFromYesterday_returnsNotFound() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        ticketFor("a@example.com", registration, 1, LocalDate.now().minusDays(1), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("You do not have an active ticket."));
    }

    @Test
    void doneTicket_isNotCountedAhead() throws Exception {
        ServiceType registration = createService("Registration", "R");
        ticketFor("done@example.com", registration, 1, LocalDate.now(), TicketStatus.DONE);
        ticketFor("a@example.com", registration, 2, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.peopleAhead").value(0));
    }

    @Test
    void getActiveTicket_waitingWithOpenCounter_hasOpenCounterTrue() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasOpenCounter").value(true))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));
    }

    @Test
    void getActiveTicket_waitingWithoutOpenCounter_hasOpenCounterFalseAndNoEstimate() throws Exception {
        ServiceType registration = createService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        window.setActive(false);
        counterRepository.save(window);
        ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.hasOpenCounter").value(false))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.peopleAhead").value(0))
                .andExpect(jsonPath("$.estimatedWaitMinutes").isEmpty());
    }

    @Test
    void getActiveTicket_calledTicket_hasOpenCounterTrue() throws Exception {
        ServiceType registration = createService("Registration", "R");
        ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.CALLED);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.hasOpenCounter").value(true));
    }

    @Test
    void getActiveTicket_serviceDeactivated_hasOpenCounterFalse() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.WAITING);
        registration.setActive(false);
        serviceTypeRepository.save(registration);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasOpenCounter").value(false))
                .andExpect(jsonPath("$.estimatedWaitMinutes").isEmpty());
    }

    @Test
    void withoutToken_getsUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void staff_getsForbidden() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + staffToken()))
                .andExpect(status().isForbidden());
    }
}
