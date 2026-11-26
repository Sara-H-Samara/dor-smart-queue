package com.sara.backend.controllers;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.Role;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketDistance;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.entities.User;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Active ticket of a student")
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
        ticketRepository.deleteAll();
    }

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

    @Test
    @DisplayName("A student without an active ticket gets not found")
    void getActiveTicket_withoutActiveTicket_returnsNotFound() throws Exception {
        registerStudentAndLogin("a@example.com");

        me("a@example.com")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("You do not have an active ticket."));
    }

    @Test
    @DisplayName("The first student in line is at position one")
    void getActiveTicket_whenFirstInLine_returnsPositionOne() throws Exception {
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

    private void saveDistance(Ticket ticket, TicketDistance distance) {
        ticket.setDistance(distance);
        ticketRepository.save(ticket);
    }

    @Test
    @DisplayName("A waiting ticket answers with its distance")
    void getActiveTicket_withWaitingFarTicket_returnsFarDistance() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        saveDistance(ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.WAITING),
                TicketDistance.FAR);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distance").value("FAR"));
    }

    @Test
    @DisplayName("A called ticket answers with its distance")
    void getActiveTicket_withCalledFarTicket_returnsFarDistance() throws Exception {
        ServiceType registration = createService("Registration", "R");
        saveDistance(ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.CALLED),
                TicketDistance.FAR);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distance").value("FAR"));
    }

    @Test
    @DisplayName("A ticket saved without a distance answers with distance near")
    void getActiveTicket_withTicketSavedWithoutDistance_returnsNearDistance() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distance").value("NEAR"));
    }

    private List<Ticket> waitingTicketsAhead(ServiceType service, int count) throws Exception {
        List<Ticket> ticketsAhead = new ArrayList<>();
        for (int number = 1; number <= count; number++) {
            ticketsAhead.add(ticketFor(
                    "ahead" + number + "@example.com", service, number, LocalDate.now(), TicketStatus.WAITING));
        }
        return ticketsAhead;
    }

    private void waitingTicketWithDistance(
            ServiceType service, int number, TicketDistance distance) throws Exception {
        saveDistance(ticketFor("student@example.com", service, number, LocalDate.now(), TicketStatus.WAITING),
                distance);
    }

    @Test
    @DisplayName("A far student with six people ahead is told the turn is near")
    void getActiveTicket_farWithSixAhead_returnsTurnIsNearTrue() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        waitingTicketsAhead(registration, 6);
        waitingTicketWithDistance(registration, 7, TicketDistance.FAR);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peopleAhead").value(6))
                .andExpect(jsonPath("$.turnIsNear").value(true));
    }

    @Test
    @DisplayName("A far student with seven people ahead is not told yet")
    void getActiveTicket_farWithSevenAhead_returnsTurnIsNearFalse() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        waitingTicketsAhead(registration, 7);
        waitingTicketWithDistance(registration, 8, TicketDistance.FAR);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peopleAhead").value(7))
                .andExpect(jsonPath("$.turnIsNear").value(false));
    }

    @Test
    @DisplayName("A near student with six people ahead is not told yet")
    void getActiveTicket_nearWithSixAhead_returnsTurnIsNearFalse() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        waitingTicketsAhead(registration, 6);
        waitingTicketWithDistance(registration, 7, TicketDistance.NEAR);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnIsNear").value(false));
    }

    @Test
    @DisplayName("A medium student with three people ahead is told the turn is near")
    void getActiveTicket_mediumWithThreeAhead_returnsTurnIsNearTrue() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        waitingTicketsAhead(registration, 3);
        waitingTicketWithDistance(registration, 4, TicketDistance.MEDIUM);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnIsNear").value(true));
    }

    @Test
    @DisplayName("The alert turns on when one person ahead leaves the queue")
    void getActiveTicket_afterPersonAheadLeaves_returnsTurnIsNearTrue() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        List<Ticket> ticketsAhead = waitingTicketsAhead(registration, 7);
        waitingTicketWithDistance(registration, 8, TicketDistance.FAR);
        me("student@example.com").andExpect(jsonPath("$.turnIsNear").value(false));

        Ticket leavingTicket = ticketsAhead.get(0);
        leavingTicket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(leavingTicket);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peopleAhead").value(6))
                .andExpect(jsonPath("$.turnIsNear").value(true));
    }

    @Test
    @DisplayName("No alert is sent when no counter is open")
    void getActiveTicket_waitingWithoutOpenCounter_returnsTurnIsNearFalse() throws Exception {
        ServiceType registration = createService("Registration", "R");
        waitingTicketWithDistance(registration, 1, TicketDistance.FAR);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasOpenCounter").value(false))
                .andExpect(jsonPath("$.turnIsNear").value(false));
    }

    @Test
    @DisplayName("A called ticket has no alert")
    void getActiveTicket_calledTicket_returnsTurnIsNearFalseAndNoAlertLimit() throws Exception {
        ServiceType registration = createService("Registration", "R");
        saveDistance(ticketFor("student@example.com", registration, 1, LocalDate.now(), TicketStatus.CALLED),
                TicketDistance.FAR);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnIsNear").value(false))
                .andExpect(jsonPath("$.alertAtPeopleAhead").isEmpty());
    }

    @Test
    @DisplayName("A far ticket tells the student the alert comes at six people ahead")
    void getActiveTicket_withFarDistance_returnsAlertAtPeopleAheadSix() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        waitingTicketWithDistance(registration, 1, TicketDistance.FAR);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertAtPeopleAhead").value(6));
    }

    @Test
    @DisplayName("A near ticket tells the student the alert comes at one person ahead")
    void getActiveTicket_withNearDistance_returnsAlertAtPeopleAheadOne() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        waitingTicketWithDistance(registration, 1, TicketDistance.NEAR);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertAtPeopleAhead").value(1));
    }

    @Test
    @DisplayName("Two open counters halve the wait estimate")
    void getActiveTicket_withTwoOpenCounters_returnsHalvedEstimate() throws Exception {
        ServiceType registration = createService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.createCounter("Window 2", registration);
        finishedTicketsOfTenMinutes(registration, window);
        ticketFor("a@example.com", registration, 4, LocalDate.now(), TicketStatus.WAITING);
        ticketFor("b@example.com", registration, 5, LocalDate.now(), TicketStatus.WAITING);

        me("b@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(10));
    }

    @Test
    @DisplayName("Two open counters double the people-ahead number of the alert")
    void getActiveTicket_withTwoOpenCounters_returnsDoubledAlertLimit() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        testHelper.createCounter("Window 2", registration);
        waitingTicketWithDistance(registration, 1, TicketDistance.FAR);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertAtPeopleAhead").value(12));
    }

    @Test
    @DisplayName("A medium student with six people ahead and two open counters is told the turn is near")
    void getActiveTicket_mediumWithSixAheadAndTwoOpenCounters_returnsTurnIsNearTrue() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        testHelper.createCounter("Window 2", registration);
        waitingTicketsAhead(registration, 6);
        waitingTicketWithDistance(registration, 7, TicketDistance.MEDIUM);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peopleAhead").value(6))
                .andExpect(jsonPath("$.turnIsNear").value(true));
    }

    @Test
    @DisplayName("When every counter is closed there is no estimate and no alert limit")
    void getActiveTicket_whenAllCountersClosed_returnsNoEstimateAndNoAlertLimit() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createInactiveCounter("Window 1", registration);
        testHelper.createInactiveCounter("Window 2", registration);
        waitingTicketWithDistance(registration, 1, TicketDistance.FAR);

        me("student@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasOpenCounter").value(false))
                .andExpect(jsonPath("$.turnIsNear").value(false))
                .andExpect(jsonPath("$.estimatedWaitMinutes").isEmpty())
                .andExpect(jsonPath("$.alertAtPeopleAhead").isEmpty());
    }

    @Test
    @DisplayName("Opening a second counter makes the wait estimate shorter")
    void getActiveTicket_afterSecondCounterOpens_returnsShorterEstimate() throws Exception {
        ServiceType registration = createService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        Counter secondWindow = testHelper.createInactiveCounter("Window 2", registration);
        finishedTicketsOfTenMinutes(registration, window);
        ticketFor("a@example.com", registration, 4, LocalDate.now(), TicketStatus.WAITING);
        ticketFor("b@example.com", registration, 5, LocalDate.now(), TicketStatus.WAITING);
        me("b@example.com").andExpect(jsonPath("$.estimatedWaitMinutes").value(20));

        secondWindow.setActive(true);
        counterRepository.save(secondWindow);

        me("b@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(10));
    }

    private void finishedTicketsOfTenMinutes(ServiceType service, Counter counter) throws Exception {
        testHelper.registerStudentAndLogin("done@example.com");
        for (int number = 1; number <= 3; number++) {
            testHelper.createFinishedTicketForRegisteredStudent(
                    "done@example.com", service, number, counter, Duration.ofMinutes(10));
        }
    }

    @Test
    @DisplayName("The third student in line is at position three")
    void getActiveTicket_whenThirdInLine_returnsPositionThree() throws Exception {
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
    @DisplayName("Enough finished tickets of the service set the wait from their average time")
    void getActiveTicket_withThreeFinishedTicketsOfTenMinutes_returnsAverageTimesPosition() throws Exception {
        ServiceType registration = createService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        finishedTicketsOfTenMinutes(registration, window);
        ticketFor("a@example.com", registration, 4, LocalDate.now(), TicketStatus.WAITING);
        ticketFor("b@example.com", registration, 5, LocalDate.now(), TicketStatus.WAITING);

        me("b@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(20));
    }

    @Test
    @DisplayName("Only the latest ten finished tickets set the wait")
    void getActiveTicket_withMoreThanTenFinishedTickets_usesOnlyTheLatestTen() throws Exception {
        ServiceType registration = createService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.registerStudentAndLogin("done@example.com");
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        testHelper.createFinishedTicketForRegisteredStudent(
                "done@example.com", registration, 1, window, twoHoursAgo.minusMinutes(50), twoHoursAgo);
        for (int number = 2; number <= 11; number++) {
            testHelper.createFinishedTicketForRegisteredStudent(
                    "done@example.com", registration, number, window, Duration.ofMinutes(10));
        }
        ticketFor("a@example.com", registration, 12, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(10));
    }

    @Test
    @DisplayName("Too few finished tickets keep the default wait")
    void getActiveTicket_withTwoFinishedTickets_returnsDefaultMinutesTimesPosition() throws Exception {
        ServiceType registration = createService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.registerStudentAndLogin("done@example.com");
        for (int number = 1; number <= 2; number++) {
            testHelper.createFinishedTicketForRegisteredStudent(
                    "done@example.com", registration, number, window, Duration.ofMinutes(10));
        }
        ticketFor("a@example.com", registration, 3, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));
    }

    @Test
    @DisplayName("Finished tickets of another service do not change the wait")
    void getActiveTicket_withFinishedTicketsOfOtherService_returnsDefaultMinutesTimesPosition() throws Exception {
        ServiceType registration = createService("Registration", "R");
        ServiceType documents = createService("Documents", "D");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.registerStudentAndLogin("done@example.com");
        for (int number = 1; number <= 3; number++) {
            testHelper.createFinishedTicketForRegisteredStudent(
                    "done@example.com", documents, number, window, Duration.ofMinutes(10));
        }
        ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));
    }

    @Test
    @DisplayName("A called ticket has no position and no wait estimate")
    void getActiveTicket_withCalledTicket_returnsNoPositionOrEstimate() throws Exception {
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
    @DisplayName("A ticket of another service is not counted ahead")
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
    @DisplayName("A ticket from an earlier day is not counted ahead")
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
    @DisplayName("A waiting ticket from yesterday is not an active ticket")
    void getActiveTicket_withOnlyWaitingTicketFromYesterday_returnsNotFound() throws Exception {
        ServiceType registration = createService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        ticketFor("a@example.com", registration, 1, LocalDate.now().minusDays(1), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("You do not have an active ticket."));
    }

    @Test
    @DisplayName("A done ticket is not counted ahead")
    void getActiveTicket_withDoneTicketAhead_doesNotCountItAhead() throws Exception {
        ServiceType registration = createService("Registration", "R");
        ticketFor("done@example.com", registration, 1, LocalDate.now(), TicketStatus.DONE);
        ticketFor("a@example.com", registration, 2, LocalDate.now(), TicketStatus.WAITING);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.peopleAhead").value(0));
    }

    @Test
    @DisplayName("A waiting ticket with an open counter shows a wait estimate")
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
    @DisplayName("A waiting ticket with no open counter has no wait estimate")
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
    @DisplayName("A called ticket always shows an open counter")
    void getActiveTicket_calledTicket_hasOpenCounterTrue() throws Exception {
        ServiceType registration = createService("Registration", "R");
        ticketFor("a@example.com", registration, 1, LocalDate.now(), TicketStatus.CALLED);

        me("a@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.hasOpenCounter").value(true));
    }

    @Test
    @DisplayName("A ticket of a deactivated service shows no open counter")
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
    @DisplayName("A visitor without a token cannot see an active ticket")
    void getActiveTicket_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Staff cannot see a student ticket")
    void getActiveTicket_asStaff_returnsForbidden() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + staffToken()))
                .andExpect(status().isForbidden());
    }
}
