package com.sara.backend.controller;

import com.sara.backend.config.DataSeeder;
import com.sara.backend.entity.Counter;
import com.sara.backend.entity.ServiceType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
    void listWaitingTickets_withNoWaitingTickets_returnsEmptyListAndZeroCount() throws Exception {
        testHelper.getWaitingList(staffToken, deskOne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingCount").value(0))
                .andExpect(jsonPath("$.tickets.length()").value(0));
    }

    @Test
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
    void listWaitingTickets_forUnknownCounter_returnsNotFoundWithMessage() throws Exception {
        testHelper.getWaitingList(staffToken, UNKNOWN_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Counter not found."));
    }

    @Test
    void listWaitingTickets_returnsOnlyDocumentedFields() throws Exception {
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
    void listWaitingTickets_asStudent_returnsForbidden() throws Exception {
        testHelper.getWaitingList(studentToken, deskOne.getId()).andExpect(status().isForbidden());
    }

    @Test
    void listWaitingTickets_asAdmin_returnsForbidden() throws Exception {
        testHelper.getWaitingList(testHelper.loginAsAdmin(), deskOne.getId()).andExpect(status().isForbidden());
    }

    @Test
    void listWaitingTickets_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/counters/{counterId}/waiting", deskOne.getId()))
                .andExpect(status().isUnauthorized());
    }
}
