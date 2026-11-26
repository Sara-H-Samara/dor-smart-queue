package com.sara.backend.security;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.Role;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.streams.QueueStreamRegistry;
import com.sara.backend.support.StreamTestHelper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Token role checked in the database")
class TokenRoleCheckTest {

    private static final String ADMIN_EMAIL = "admin@dor.com";
    private static final String ADMIN_PASSWORD = "admin12345";
    private static final String STAFF_EMAIL = "test-staff@example.com";
    private static final String STAFF_PASSWORD = "staff12345";
    private static final String STUDENT_EMAIL = "student@example.com";
    private static final String ADMIN_SERVICES_URL = "/api/admin/services";
    private static final String STAFF_STREAM_URL = "/api/staff/stream";
    private static final String WAITING_ROOM_URL = "/api/public/queue-status";

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

    @Autowired
    private QueueStreamRegistry queueStreamRegistry;

    private TicketTestHelper testHelper;
    private StreamTestHelper streamTestHelper;

    @BeforeEach
    void resetDatabaseWithSeededUsers() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        streamTestHelper = new StreamTestHelper(queueStreamRegistry);
    }

    @AfterEach
    void closeOpenStreams() {
        streamTestHelper.close();
    }

    @Test
    void listAdminServices_withTokenOfDemotedAdmin_returnsUnauthorized() throws Exception {
        String adminToken = testHelper.loginAsAdmin();
        testHelper.updateRoleInDatabase(ADMIN_EMAIL, Role.STAFF);

        listAdminServices(adminToken).andExpect(status().isUnauthorized());
    }

    @Test
    void listAdminServices_withTokenIssuedBeforePromotionToAdmin_returnsUnauthorized() throws Exception {
        String staffToken = testHelper.loginAsNewStaff();
        testHelper.updateRoleInDatabase(STAFF_EMAIL, Role.ADMIN);

        listAdminServices(staffToken).andExpect(status().isUnauthorized());
    }

    @Test
    void listAdminServices_withNewTokenAfterPromotion_returnsOk() throws Exception {
        testHelper.loginAsNewStaff();
        testHelper.updateRoleInDatabase(STAFF_EMAIL, Role.ADMIN);
        String newToken = testHelper.loginAndGetToken(STAFF_EMAIL, STAFF_PASSWORD);

        listAdminServices(newToken).andExpect(status().isOk());
    }

    @Test
    void listAdminServices_withTokenFromUpperCaseLogin_returnsOk() throws Exception {
        String adminToken = testHelper.loginAndGetToken(ADMIN_EMAIL.toUpperCase(), ADMIN_PASSWORD);

        listAdminServices(adminToken).andExpect(status().isOk());
    }

    @Test
    void takeTicket_withTokenOfStudentPromotedToStaff_returnsUnauthorized() throws Exception {
        ServiceType service = testHelper.createActiveService("Registration", "R");
        testHelper.createCounter("Counter 1", service);
        String studentToken = testHelper.registerStudentAndLogin(STUDENT_EMAIL);
        testHelper.updateRoleInDatabase(STUDENT_EMAIL, Role.STAFF);

        testHelper.takeTicket(studentToken, service.getId()).andExpect(status().isUnauthorized());
    }

    @Test
    void getWaitingList_withTokenOfDemotedStaff_returnsUnauthorized() throws Exception {
        ServiceType service = testHelper.createActiveService("Registration", "R");
        Counter counter = testHelper.createCounter("Counter 1", service);
        String staffToken = testHelper.loginAsNewStaff();
        testHelper.updateRoleInDatabase(STAFF_EMAIL, Role.STUDENT);

        testHelper.getWaitingList(staffToken, counter.getId()).andExpect(status().isUnauthorized());
    }

    @Test
    void getMyActiveTicket_withTokenOfDeletedUser_returnsUnauthorized() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin(STUDENT_EMAIL);
        userRepository.delete(userRepository.findByEmail(STUDENT_EMAIL).orElseThrow());

        testHelper.getMyActiveTicket(studentToken).andExpect(status().isUnauthorized());
    }

    @Test
    void listAdminServices_withTokenOfDeletedAdmin_returnsUnauthorized() throws Exception {
        String adminToken = testHelper.loginAsAdmin();
        userRepository.delete(userRepository.findByEmail(ADMIN_EMAIL).orElseThrow());

        listAdminServices(adminToken).andExpect(status().isUnauthorized());
    }

    @Test
    void openStaffStream_withTokenOfDemotedStaff_returnsUnauthorized() throws Exception {
        String staffToken = testHelper.loginAsNewStaff();
        testHelper.updateRoleInDatabase(STAFF_EMAIL, Role.STUDENT);

        streamTestHelper.performStreamRequest(mockMvc, get(STAFF_STREAM_URL)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withStaleTokenHeader_returnsOk() throws Exception {
        String adminToken = testHelper.loginAsAdmin();
        testHelper.updateRoleInDatabase(ADMIN_EMAIL, Role.STAFF);

        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    void getWaitingRoom_withStaleToken_returnsOk() throws Exception {
        String adminToken = testHelper.loginAsAdmin();
        testHelper.updateRoleInDatabase(ADMIN_EMAIL, Role.STAFF);

        mockMvc.perform(get(WAITING_ROOM_URL).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private ResultActions listAdminServices(String token) throws Exception {
        return mockMvc.perform(get(ADMIN_SERVICES_URL).header("Authorization", "Bearer " + token));
    }
}
