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
import org.junit.jupiter.api.DisplayName;
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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Public waiting room")
class WaitingRoomControllerTest {

    private static final String WAITING_ROOM_URL = "/api/public/queue-status";
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
    void resetDatabaseWithSeededAdmin() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        testHelper.registerStudentAndLogin(STUDENT_EMAIL);
    }

    @AfterEach
    void deleteAllTickets() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Anyone can open the waiting room without logging in")
    void showWaitingRoom_withoutToken_returnsOk() throws Exception {
        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("An invalid token does not block the waiting room")
    void showWaitingRoom_withInvalidToken_returnsOk() throws Exception {
        mockMvc.perform(get(WAITING_ROOM_URL).header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A counter shows the code of the ticket it is serving")
    void showWaitingRoom_withCalledTicket_returnsCounterWithTicketCode() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.createCalledTicketForRegisteredStudent(STUDENT_EMAIL, registration, 7, window);

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counters.length()").value(1))
                .andExpect(jsonPath("$.counters[0].counterName").value("Window 1"))
                .andExpect(jsonPath("$.counters[0].serviceName").value("Registration"))
                .andExpect(jsonPath("$.counters[0].ticketCode").value("R-007"));
    }

    @Test
    @DisplayName("A counter that serves nobody has no ticket code")
    void showWaitingRoom_counterWithoutCalledTicket_returnsNullTicketCode() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        testHelper.createCounter("Window 1", registration);

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counters.length()").value(1))
                .andExpect(jsonPath("$.counters[0].ticketCode").value(nullValue()));
    }

    @Test
    @DisplayName("An inactive counter is hidden")
    void showWaitingRoom_withInactiveCounter_hidesIt() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        Counter closedWindow = testHelper.createCounter("Window 2", registration);
        closedWindow.setActive(false);
        counterRepository.save(closedWindow);

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counters.length()").value(1))
                .andExpect(jsonPath("$.counters[0].counterName").value("Window 1"));
    }

    @Test
    @DisplayName("Counters and waiting tickets of an inactive service are hidden")
    void showWaitingRoom_withInactiveService_hidesItsCountersAndWaitingTickets() throws Exception {
        ServiceType archive = testHelper.createInactiveService("Archive", "A");
        testHelper.createCounter("Archive window", archive);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, archive, 1, TicketStatus.WAITING);

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counters.length()").value(0))
                .andExpect(jsonPath("$.nextTickets.length()").value(0))
                .andExpect(jsonPath("$.moreWaitingCount").value(0));
    }

    @Test
    @DisplayName("Counters are listed in the order they were created")
    void showWaitingRoom_withCountersCreatedInOrder_returnsThemSortedById() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        testHelper.createCounter("Counter 2", registration);
        testHelper.createCounter("Counter 10", registration);

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counters[0].counterName").value("Counter 2"))
                .andExpect(jsonPath("$.counters[1].counterName").value("Counter 10"));
    }

    @Test
    @DisplayName("Only the three oldest waiting tickets are listed, the rest are counted")
    void showWaitingRoom_withFiveWaitingTicketsInTwoServices_returnsFirstThreeOldestFirst() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        ServiceType documents = testHelper.createActiveService("Documents", "D");
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, TicketStatus.WAITING);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, documents, 1, TicketStatus.WAITING);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 2, TicketStatus.WAITING);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, documents, 2, TicketStatus.WAITING);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 3, TicketStatus.WAITING);

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextTickets.length()").value(3))
                .andExpect(jsonPath("$.nextTickets[0].ticketCode").value("R-001"))
                .andExpect(jsonPath("$.nextTickets[0].serviceName").value("Registration"))
                .andExpect(jsonPath("$.nextTickets[1].ticketCode").value("D-001"))
                .andExpect(jsonPath("$.nextTickets[1].serviceName").value("Documents"))
                .andExpect(jsonPath("$.nextTickets[2].ticketCode").value("R-002"))
                .andExpect(jsonPath("$.moreWaitingCount").value(2));
    }

    @Test
    @DisplayName("With three waiting tickets or fewer, the count of more waiting is zero")
    void showWaitingRoom_withThreeOrFewerWaitingTickets_returnsZeroMoreWaiting() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, TicketStatus.WAITING);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 2, TicketStatus.WAITING);

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextTickets.length()").value(2))
                .andExpect(jsonPath("$.moreWaitingCount").value(0));
    }

    @Test
    @DisplayName("Only waiting tickets are listed as next")
    void showWaitingRoom_withTicketsInEveryStatus_listsOnlyWaitingTicketsAsNext() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, TicketStatus.WAITING);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 2, TicketStatus.DONE);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 3, TicketStatus.CANCELLED);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 4, TicketStatus.NO_SHOW);
        testHelper.createCalledTicketForRegisteredStudent(STUDENT_EMAIL, registration, 5, window);

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextTickets.length()").value(1))
                .andExpect(jsonPath("$.nextTickets[0].ticketCode").value("R-001"))
                .andExpect(jsonPath("$.moreWaitingCount").value(0));
    }

    @Test
    @DisplayName("Tickets from yesterday are ignored")
    void showWaitingRoom_withTicketsOfYesterday_ignoresThem() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        testHelper.createCalledTicketOnDateForRegisteredStudent(STUDENT_EMAIL, registration, 1, window, yesterday);
        testHelper.createTicketOnDateForRegisteredStudent(
                STUDENT_EMAIL, registration, 2, yesterday, TicketStatus.WAITING);

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counters[0].ticketCode").value(nullValue()))
                .andExpect(jsonPath("$.nextTickets.length()").value(0))
                .andExpect(jsonPath("$.moreWaitingCount").value(0));
    }

    @Test
    @DisplayName("A counter shows the ticket it called last")
    void showWaitingRoom_counterWithTwoCalledTickets_showsNewestCall() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        Ticket calledEarlier = testHelper.createCalledTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 1, window);
        Ticket calledLater = testHelper.createCalledTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 2, window);
        calledEarlier.setCalledAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        calledLater.setCalledAt(LocalDateTime.of(2026, 1, 1, 9, 5));
        ticketRepository.save(calledEarlier);
        ticketRepository.save(calledLater);

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counters.length()").value(1))
                .andExpect(jsonPath("$.counters[0].ticketCode").value("R-002"));
    }

    @Test
    @DisplayName("With no counters, all lists are empty")
    void showWaitingRoom_withNoCounters_returnsEmptyLists() throws Exception {
        testHelper.createActiveService("Registration", "R");

        mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counters.length()").value(0))
                .andExpect(jsonPath("$.nextTickets.length()").value(0))
                .andExpect(jsonPath("$.moreWaitingCount").value(0));
    }

    @Test
    @DisplayName("The waiting room shows no personal data")
    void showWaitingRoom_withCalledAndWaitingTickets_returnsOnlySafeFields() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        testHelper.createCalledTicketForRegisteredStudent(STUDENT_EMAIL, registration, 7, window);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 8, TicketStatus.WAITING);

        String responseBody = mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> waitingRoomFields = JsonPath.read(responseBody, "$");
        Map<String, Object> counterFields = JsonPath.read(responseBody, "$.counters[0]");
        Map<String, Object> nextTicketFields = JsonPath.read(responseBody, "$.nextTickets[0]");
        assertThat(waitingRoomFields.keySet())
                .containsExactlyInAnyOrder("counters", "nextTickets", "moreWaitingCount");
        assertThat(counterFields.keySet()).containsExactlyInAnyOrder("counterName", "serviceName", "ticketCode");
        assertThat(nextTicketFields.keySet()).containsExactlyInAnyOrder("ticketCode", "serviceName");
        assertThat(responseBody).doesNotContain(STUDENT_EMAIL).doesNotContain("Student");
    }

    @Test
    @DisplayName("A counter that serves nobody still has a ticket code field")
    void showWaitingRoom_counterWithoutCalledTicket_keepsTicketCodeKeyWithNull() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        testHelper.createCounter("Window 1", registration);

        String responseBody = mockMvc.perform(get(WAITING_ROOM_URL))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> counterFields = JsonPath.read(responseBody, "$.counters[0]");
        assertThat(counterFields).containsKey("ticketCode");
    }

    @Test
    @DisplayName("A sub path of the waiting room needs a token")
    void showWaitingRoom_withSubPathAndNoToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(WAITING_ROOM_URL + "/anything"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Posting to the waiting room needs a token")
    void showWaitingRoom_withPostMethodAndNoToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(WAITING_ROOM_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("The current student of a counter needs a token")
    void getCurrentStudent_withoutToken_returnsUnauthorized() throws Exception {
        Counter window = testHelper.createCounter("Window 1", testHelper.createActiveService("Registration", "R"));

        mockMvc.perform(get("/api/counters/{counterId}/current", window.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("The waiting list of a counter needs a token")
    void getWaitingList_withoutToken_returnsUnauthorized() throws Exception {
        Counter window = testHelper.createCounter("Window 1", testHelper.createActiveService("Registration", "R"));

        mockMvc.perform(get("/api/counters/{counterId}/waiting", window.getId()))
                .andExpect(status().isUnauthorized());
    }
}
