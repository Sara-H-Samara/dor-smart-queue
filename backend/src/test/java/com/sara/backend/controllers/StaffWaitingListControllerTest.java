package com.sara.backend.controllers;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.ServiceType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Waiting list for staff")
class StaffWaitingListControllerTest {

    private static final String STUDENT_EMAIL = "student@example.com";
    private static final long UNKNOWN_ID = 999999L;
    private static final int WAITING_LIST_LIMIT = 20;

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
    private String studentToken;

    @BeforeEach
    void resetDatabaseWithStaffStudentAndCounter() throws Exception {
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
        studentToken = testHelper.registerStudentAndLogin(STUDENT_EMAIL);
    }

    @AfterEach
    void deleteAllTickets() {
        ticketRepository.deleteAll();
    }

    private void createWaitingTicket(ServiceType service, int number) {
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, service, number, TicketStatus.WAITING);
    }

    @Test
    @DisplayName("Waiting tickets are listed in queue order")
    void listWaitingTickets_withWaitingTickets_returnsThemInQueueOrder() throws Exception {
        createWaitingTicket(registration, 2);
        createWaitingTicket(registration, 1);
        createWaitingTicket(registration, 3);

        testHelper.getWaitingList(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingCount").value(3))
                .andExpect(jsonPath("$.tickets.length()").value(3))
                .andExpect(jsonPath("$.tickets[0].ticketCode").value("R-001"))
                .andExpect(jsonPath("$.tickets[1].ticketCode").value("R-002"))
                .andExpect(jsonPath("$.tickets[2].ticketCode").value("R-003"));
    }

    @Test
    @DisplayName("Only the first twenty waiting tickets are listed, the count shows all")
    void listWaitingTickets_withMoreThanTwentyWaiting_returnsFirstTwentyAndFullCount() throws Exception {
        for (int number = 1; number <= WAITING_LIST_LIMIT + 5; number++) {
            createWaitingTicket(registration, number);
        }

        testHelper.getWaitingList(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingCount").value(WAITING_LIST_LIMIT + 5))
                .andExpect(jsonPath("$.tickets.length()").value(WAITING_LIST_LIMIT))
                .andExpect(jsonPath("$.tickets[0].ticketCode").value("R-001"))
                .andExpect(jsonPath("$.tickets[19].ticketCode").value("R-020"));
    }

    @Test
    @DisplayName("With no waiting tickets, the list is empty")
    void listWaitingTickets_withNoWaitingTickets_returnsEmptyListAndZeroCount() throws Exception {
        testHelper.getWaitingList(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingCount").value(0))
                .andExpect(jsonPath("$.tickets.length()").value(0));
    }

    @Test
    @DisplayName("Tickets that are not waiting are left out")
    void listWaitingTickets_withCalledDoneNoShowAndCancelledTickets_leavesThemOut() throws Exception {
        testHelper.createCalledTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, deskOne);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 2, TicketStatus.DONE);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 3, TicketStatus.NO_SHOW);
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, registration, 4, TicketStatus.CANCELLED);
        createWaitingTicket(registration, 5);

        testHelper.getWaitingList(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingCount").value(1))
                .andExpect(jsonPath("$.tickets.length()").value(1))
                .andExpect(jsonPath("$.tickets[0].ticketCode").value("R-005"));
    }

    @Test
    @DisplayName("Tickets from an earlier day are left out")
    void listWaitingTickets_withTicketsOfEarlierDay_leavesThemOutOfListAndCount() throws Exception {
        testHelper.createTicketOnDateForRegisteredStudent(
                STUDENT_EMAIL, registration, 1, LocalDate.now().minusDays(1), TicketStatus.WAITING);
        createWaitingTicket(registration, 1);

        testHelper.getWaitingList(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingCount").value(1))
                .andExpect(jsonPath("$.tickets.length()").value(1));
    }

    @Test
    @DisplayName("Tickets of another service are left out")
    void listWaitingTickets_withTicketsOfOtherService_leavesThemOut() throws Exception {
        ServiceType documents = testHelper.createActiveService("Documents", "D");
        createWaitingTicket(documents, 1);
        createWaitingTicket(registration, 1);

        testHelper.getWaitingList(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingCount").value(1))
                .andExpect(jsonPath("$.tickets[0].ticketCode").value("R-001"));
    }

    @Test
    @DisplayName("A ticket that was called is no longer in the list")
    void listWaitingTickets_afterCallNext_noLongerListsTheCalledTicket() throws Exception {
        createWaitingTicket(registration, 1);
        createWaitingTicket(registration, 2);

        testHelper.callNext(staffToken, deskOne.getId()).andExpect(status().isOk());

        testHelper.getWaitingList(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingCount").value(1))
                .andExpect(jsonPath("$.tickets.length()").value(1))
                .andExpect(jsonPath("$.tickets[0].ticketCode").value("R-002"));
    }

    @Test
    @DisplayName("An inactive counter still shows its queue")
    void listWaitingTickets_forInactiveCounter_returnsQueue() throws Exception {
        deskOne.setActive(false);
        counterRepository.save(deskOne);
        createWaitingTicket(registration, 1);

        testHelper.getWaitingList(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingCount").value(1))
                .andExpect(jsonPath("$.tickets[0].ticketCode").value("R-001"));
    }

    @Test
    @DisplayName("A counter of an inactive service still shows its queue")
    void listWaitingTickets_forInactiveService_returnsQueue() throws Exception {
        ServiceType archive = testHelper.createInactiveService("Archive", "A");
        Counter archiveDesk = testHelper.createCounter("Desk 2", archive);
        createWaitingTicket(archive, 1);

        testHelper.getWaitingList(staffToken, archiveDesk.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingCount").value(1))
                .andExpect(jsonPath("$.tickets[0].ticketCode").value("A-001"));
    }

    @Test
    @DisplayName("A counter that does not exist gives not found with a message")
    void listWaitingTickets_forUnknownCounter_returnsNotFoundWithMessage() throws Exception {
        testHelper.getWaitingList(staffToken, UNKNOWN_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Counter not found."));
    }

    @Test
    @DisplayName("The list shows only the fields from the documentation")
    void listWaitingTickets_asStaff_returnsOnlyDocumentedFields() throws Exception {
        createWaitingTicket(registration, 1);

        testHelper.getWaitingList(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.waitingCount").exists())
                .andExpect(jsonPath("$.tickets").exists())
                .andExpect(jsonPath("$.tickets[0].length()").value(2))
                .andExpect(jsonPath("$.tickets[0].ticketCode").value("R-001"))
                .andExpect(jsonPath("$.tickets[0].createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("A student cannot see the waiting list")
    void listWaitingTickets_asStudent_returnsForbidden() throws Exception {
        testHelper.getWaitingList(studentToken, deskOne.getId()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An admin cannot see the waiting list")
    void listWaitingTickets_asAdmin_returnsForbidden() throws Exception {
        testHelper.getWaitingList(testHelper.loginAsAdmin(), deskOne.getId()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A visitor without a token cannot see the waiting list")
    void listWaitingTickets_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/counters/{counterId}/waiting", deskOne.getId()))
                .andExpect(status().isUnauthorized());
    }
}
