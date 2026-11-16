package com.sara.backend.controller;

import com.sara.backend.config.DataSeeder;
import com.sara.backend.entity.Counter;
import com.sara.backend.entity.ServiceType;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiErrorResponseTest {

    private static final long UNKNOWN_ID = 999999L;
    private static final String ADMIN_COUNTERS_URL = "/api/admin/counters";

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
    private String adminToken;

    @BeforeEach
    void resetDatabaseWithStaffAndAdmin() throws Exception {
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
        adminToken = testHelper.loginAsAdmin();
    }

    @AfterEach
    void deleteAllTickets() {
        ticketRepository.deleteAll();
    }

    @Test
    void callNext_counterAlreadyServing_returnsConflictWithCounterBusyMessage() throws Exception {
        testHelper.registerStudentAndLogin("a@example.com");
        testHelper.createCalledTicketForRegisteredStudent("a@example.com", registration, 1, deskOne);

        testHelper.callNext(staffToken, deskOne.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("This counter is already serving a student."));
    }

    @Test
    void markDone_unknownTicket_returnsNotFoundWithTicketNotFoundMessage() throws Exception {
        testHelper.markDone(staffToken, UNKNOWN_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket not found."));
    }

    @Test
    void createCounter_blankName_returnsBadRequestWithFieldNamesMessage() throws Exception {
        mockMvc.perform(post(ADMIN_COUNTERS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"serviceId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for: name, serviceId."));
    }

    @Test
    void createCounter_malformedJson_returnsBadRequestWithFixedMessage() throws Exception {
        mockMvc.perform(post(ADMIN_COUNTERS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body is not valid JSON."));
    }

    @Test
    void getUnknownPath_authenticatedUser_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getCallNext_wrongMethod_returnsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/counters/{counterId}/call-next", deskOne.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    void createCounter_plainTextBody_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post(ADMIN_COUNTERS_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Desk 2"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test
    void markDone_unknownTicket_returnsExactlyFourFields() throws Exception {
        testHelper.markDone(staffToken, UNKNOWN_ID)
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").value(org.hamcrest.Matchers.matchesPattern(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")));
    }
}
