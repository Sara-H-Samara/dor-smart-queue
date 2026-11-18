package com.sara.backend.controller;

import com.jayway.jsonpath.JsonPath;
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
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceQueueStatusControllerTest {

    private static final String QUEUE_STATUS_URL = "/api/public/queue-status";
    private static final String STUDENT_EMAIL = "private-student@example.com";

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

    @Test
    void listServiceQueueStatuses_withoutToken_returnsOk() throws Exception {
        mockMvc.perform(get(QUEUE_STATUS_URL))
                .andExpect(status().isOk());
    }

    @Test
    void listServiceQueueStatuses_withInvalidToken_returnsOk() throws Exception {
        mockMvc.perform(get(QUEUE_STATUS_URL).header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isOk());
    }

    @Test
    void listServiceQueueStatuses_withCalledTicket_returnsTicketCodeAndCounterName() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.registerStudentAndLogin(STUDENT_EMAIL);
        testHelper.createCalledTicketForRegisteredStudent(STUDENT_EMAIL, registration, 7, window);

        mockMvc.perform(get(QUEUE_STATUS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].serviceName").value("Registration"))
                .andExpect(jsonPath("$[0].serviceCode").value("R"))
                .andExpect(jsonPath("$[0].calledTickets.length()").value(1))
                .andExpect(jsonPath("$[0].calledTickets[0].ticketCode").value("R-007"))
                .andExpect(jsonPath("$[0].calledTickets[0].counterName").value("Window 1"));
    }

    @Test
    void listServiceQueueStatuses_withTwoCalledTickets_returnsNewestCallFirst() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter firstWindow = testHelper.createCounter("Window 1", registration);
        Counter secondWindow = testHelper.createCounter("Window 2", registration);
        testHelper.registerStudentAndLogin(STUDENT_EMAIL);
        Ticket calledEarlier = testHelper.createCalledTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 1, firstWindow);
        Ticket calledLater = testHelper.createCalledTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 2, secondWindow);
        calledEarlier.setCalledAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        calledLater.setCalledAt(LocalDateTime.of(2026, 1, 1, 9, 5));
        ticketRepository.save(calledEarlier);
        ticketRepository.save(calledLater);

        mockMvc.perform(get(QUEUE_STATUS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].calledTickets.length()").value(2))
                .andExpect(jsonPath("$[0].calledTickets[0].ticketCode").value("R-002"))
                .andExpect(jsonPath("$[0].calledTickets[1].ticketCode").value("R-001"));
    }

    @Test
    void listServiceQueueStatuses_withCalledTicket_returnsOnlySafeFields() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.registerStudentAndLogin(STUDENT_EMAIL);
        testHelper.createCalledTicketForRegisteredStudent(STUDENT_EMAIL, registration, 7, window);

        String responseBody = mockMvc.perform(get(QUEUE_STATUS_URL))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> serviceFields = JsonPath.read(responseBody, "$[0]");
        Map<String, Object> calledTicketFields = JsonPath.read(responseBody, "$[0].calledTickets[0]");
        assertThat(serviceFields.keySet())
                .containsExactlyInAnyOrder("serviceName", "serviceCode", "waitingCount", "calledTickets");
        assertThat(calledTicketFields.keySet()).containsExactlyInAnyOrder("ticketCode", "counterName");
        assertThat(responseBody).doesNotContain(STUDENT_EMAIL).doesNotContain("Student");
    }

    @Test
    void listServiceQueueStatuses_withTicketsOfYesterday_ignoresThem() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.registerStudentAndLogin(STUDENT_EMAIL);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        testHelper.createCalledTicketOnDateForRegisteredStudent(STUDENT_EMAIL, registration, 1, window, yesterday);
        testHelper.createTicketOnDateForRegisteredStudent(
                STUDENT_EMAIL, registration, 2, yesterday, TicketStatus.WAITING);

        mockMvc.perform(get(QUEUE_STATUS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].waitingCount").value(0))
                .andExpect(jsonPath("$[0].calledTickets.length()").value(0));
    }

    @Test
    void listServiceQueueStatuses_withTicketsInEveryStatus_countsOnlyWaitingTickets() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.registerStudentAndLogin(STUDENT_EMAIL);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, TicketStatus.WAITING);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 2, TicketStatus.WAITING);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 3, TicketStatus.DONE);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 4, TicketStatus.CANCELLED);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 5, TicketStatus.NO_SHOW);
        testHelper.createCalledTicketForRegisteredStudent(STUDENT_EMAIL, registration, 6, window);

        mockMvc.perform(get(QUEUE_STATUS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].waitingCount").value(2))
                .andExpect(jsonPath("$[0].calledTickets.length()").value(1));
    }

    @Test
    void listServiceQueueStatuses_serviceWithoutCounterOrTickets_isListedWithZeroWaiting() throws Exception {
        testHelper.createActiveService("Registration", "R");

        mockMvc.perform(get(QUEUE_STATUS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].waitingCount").value(0))
                .andExpect(jsonPath("$[0].calledTickets.length()").value(0));
    }

    @Test
    void listServiceQueueStatuses_withInactiveService_hidesIt() throws Exception {
        testHelper.createActiveService("Registration", "R");
        testHelper.createInactiveService("Archive", "A");

        mockMvc.perform(get(QUEUE_STATUS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].serviceName").value("Registration"));
    }

    @Test
    void listServiceQueueStatuses_withTwoServices_returnsThemSortedByName() throws Exception {
        testHelper.createActiveService("Registration", "R");
        testHelper.createActiveService("Documents", "D");

        mockMvc.perform(get(QUEUE_STATUS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("Documents"))
                .andExpect(jsonPath("$[1].serviceName").value("Registration"));
    }

    @Test
    void listServiceQueueStatuses_withNoServices_returnsEmptyList() throws Exception {
        mockMvc.perform(get(QUEUE_STATUS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listServiceQueueStatuses_withSubPathAndNoToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(QUEUE_STATUS_URL + "/anything"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listServiceQueueStatuses_withPostMethodAndNoToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(QUEUE_STATUS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentStudent_withoutToken_returnsUnauthorized() throws Exception {
        Counter window = testHelper.createCounter("Window 1", testHelper.createActiveService("Registration", "R"));

        mockMvc.perform(get("/api/counters/{counterId}/current", window.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getWaitingList_withoutToken_returnsUnauthorized() throws Exception {
        Counter window = testHelper.createCounter("Window 1", testHelper.createActiveService("Registration", "R"));

        mockMvc.perform(get("/api/counters/{counterId}/waiting", window.getId()))
                .andExpect(status().isUnauthorized());
    }
}
