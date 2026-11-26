package com.sara.backend.controllers;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Role;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.entities.User;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.support.TicketTestHelper;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin users")
class UserRoleControllerTest {

    private static final String URL = "/api/admin/users";
    private static final String ROLE_URL = "/api/admin/users/{id}/role";
    private static final String STUDENT_EMAIL = "student@example.com";
    private static final long UNKNOWN_ID = 999_999L;

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
    private String adminToken;

    @BeforeEach
    void resetDatabaseWithSeededUsers() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        adminToken = testHelper.loginAsAdmin();
    }

    @Test
    void listUsers_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void listUsers_asStudent_returnsForbidden() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("student@example.com");

        mockMvc.perform(listUsersRequest(studentToken)).andExpect(status().isForbidden());
    }

    @Test
    void listUsers_asStaff_returnsForbidden() throws Exception {
        String staffToken = testHelper.loginAsNewStaff();

        mockMvc.perform(listUsersRequest(staffToken)).andExpect(status().isForbidden());
    }

    @Test
    void listUsers_asAdmin_returnsOk() throws Exception {
        mockMvc.perform(listUsersRequest(adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.users[0].fullName").value("Desk Staff"))
                .andExpect(jsonPath("$.users[0].email").value("staff@dor.com"))
                .andExpect(jsonPath("$.users[0].role").value("STAFF"))
                .andExpect(jsonPath("$.users[0].id").isNumber())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listUsers_asAdmin_neverReturnsPasswordHash() throws Exception {
        String responseBody = mockMvc.perform(listUsersRequest(adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(responseBody.toLowerCase().contains("password"));
        assertFalse(responseBody.contains("$2a$"));
    }

    @Test
    void listUsers_asAdmin_marksOwnRowAsCurrentUser() throws Exception {
        mockMvc.perform(listUsersRequest(adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].email").value("staff@dor.com"))
                .andExpect(jsonPath("$.users[0].currentUser").value(false))
                .andExpect(jsonPath("$.users[1].email").value("admin@dor.com"))
                .andExpect(jsonPath("$.users[1].currentUser").value(true));
    }

    @Test
    void listUsers_asAdmin_returnsUsersSortedByNameThenId() throws Exception {
        testHelper.createUserInDatabase("Bea", "zz-bea@example.com", Role.STUDENT);
        testHelper.createUserInDatabase("Adam", "adam@example.com", Role.STUDENT);
        testHelper.createUserInDatabase("Bea", "aa-bea@example.com", Role.STUDENT);

        mockMvc.perform(listUsersRequest(adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].email").value("adam@example.com"))
                .andExpect(jsonPath("$.users[1].email").value("zz-bea@example.com"))
                .andExpect(jsonPath("$.users[2].email").value("aa-bea@example.com"))
                .andExpect(jsonPath("$.users[3].fullName").value("Desk Staff"))
                .andExpect(jsonPath("$.users[4].fullName").value("Office Admin"));
    }

    @Test
    void listUsers_withSortParameter_keepsNameOrder() throws Exception {
        testHelper.createUserInDatabase("Zoe", "zoe@example.com", Role.STUDENT);
        testHelper.createUserInDatabase("Adam", "adam@example.com", Role.STUDENT);

        mockMvc.perform(listUsersRequest(adminToken).param("sort", "fullName,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].fullName").value("Adam"))
                .andExpect(jsonPath("$.users[3].fullName").value("Zoe"));
    }

    @Test
    void listUsers_withSearch_returnsMatchingNameOrEmailOnly() throws Exception {
        testHelper.createUserInDatabase("Ann Lee", "lee@example.com", Role.STUDENT);
        testHelper.createUserInDatabase("Bob Stone", "joann@example.com", Role.STUDENT);
        testHelper.createUserInDatabase("Carl", "carl@example.com", Role.STUDENT);

        mockMvc.perform(listUsersRequest(adminToken).param("search", "ann"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.users[0].fullName").value("Ann Lee"))
                .andExpect(jsonPath("$.users[1].fullName").value("Bob Stone"));
    }

    @Test
    void listUsers_withSearchInOtherLetterCase_returnsMatchingUser() throws Exception {
        testHelper.createUserInDatabase("Ann Lee", "lee@example.com", Role.STUDENT);

        mockMvc.perform(listUsersRequest(adminToken).param("search", "aNN LEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1))
                .andExpect(jsonPath("$.users[0].email").value("lee@example.com"));
    }

    @Test
    void listUsers_withSearchContainingPercentSign_matchesItLiterally() throws Exception {
        testHelper.createUserInDatabase("Sure 100% Student", "sure@example.com", Role.STUDENT);
        testHelper.createUserInDatabase("Plain Student", "plain@example.com", Role.STUDENT);

        mockMvc.perform(listUsersRequest(adminToken).param("search", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1))
                .andExpect(jsonPath("$.users[0].email").value("sure@example.com"));
    }

    @Test
    void listUsers_withPageAndSize_returnsThatPageAndTotals() throws Exception {
        testHelper.createUserInDatabase("A1", "a1@example.com", Role.STUDENT);
        testHelper.createUserInDatabase("A2", "a2@example.com", Role.STUDENT);
        testHelper.createUserInDatabase("A3", "a3@example.com", Role.STUDENT);

        mockMvc.perform(listUsersRequest(adminToken).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.users[0].fullName").value("A3"))
                .andExpect(jsonPath("$.users[1].fullName").value("Desk Staff"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalUsers").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void listUsers_withNegativePage_returnsBadRequest() throws Exception {
        mockMvc.perform(listUsersRequest(adminToken).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page must be 0 or more."));
    }

    @Test
    void listUsers_withSizeAboveLimit_returnsBadRequest() throws Exception {
        mockMvc.perform(listUsersRequest(adminToken).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be 1 to 100."));
    }

    @Test
    void listUsers_withZeroSize_returnsBadRequest() throws Exception {
        mockMvc.perform(listUsersRequest(adminToken).param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be 1 to 100."));
    }

    @Test
    void listUsers_withTooLongSearch_returnsBadRequest() throws Exception {
        mockMvc.perform(listUsersRequest(adminToken).param("search", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Search must be at most 100 characters."));
    }

    @Test
    void changeRole_withoutToken_returnsUnauthorized() throws Exception {
        User student = testHelper.createUserInDatabase("Student", STUDENT_EMAIL, Role.STUDENT);

        mockMvc.perform(patch(ROLE_URL, student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleJson("STAFF")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changeRole_asStudent_returnsForbidden() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin(STUDENT_EMAIL);
        User other = testHelper.createUserInDatabase("Other", "other@example.com", Role.STUDENT);

        mockMvc.perform(changeRoleRequest(studentToken, other.getId(), roleJson("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeRole_asStaff_returnsForbidden() throws Exception {
        String staffToken = testHelper.loginAsNewStaff();
        User student = testHelper.createUserInDatabase("Student", STUDENT_EMAIL, Role.STUDENT);

        mockMvc.perform(changeRoleRequest(staffToken, student.getId(), roleJson("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeRole_asAdmin_savesNewRole() throws Exception {
        User student = testHelper.createUserInDatabase("Student", STUDENT_EMAIL, Role.STUDENT);

        mockMvc.perform(changeRoleRequest(adminToken, student.getId(), roleJson("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId()))
                .andExpect(jsonPath("$.fullName").value("Student"))
                .andExpect(jsonPath("$.email").value(STUDENT_EMAIL))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.currentUser").value(false));

        assertEquals(Role.STAFF, roleInDatabase(student.getId()));
    }

    @Test
    void changeRole_ofOwnAccount_returnsConflict() throws Exception {
        User admin = userRepository.findByEmail("admin@dor.com").orElseThrow();

        mockMvc.perform(changeRoleRequest(adminToken, admin.getId(), roleJson("STAFF")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You cannot change your own role."));

        assertEquals(Role.ADMIN, roleInDatabase(admin.getId()));
    }

    @Test
    void changeRole_ofOwnAccountWhenCallerIsTheOnlyAdmin_returnsConflict() throws Exception {
        User admin = userRepository.findByEmail("admin@dor.com").orElseThrow();
        assertEquals(1, userRepository.findAll().stream().filter(user -> user.getRole() == Role.ADMIN).count());

        mockMvc.perform(changeRoleRequest(adminToken, admin.getId(), roleJson("STUDENT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You cannot change your own role."));

        assertEquals(Role.ADMIN, roleInDatabase(admin.getId()));
    }

    @Test
    void changeRole_toSameRole_returnsUnchangedUser() throws Exception {
        User student = testHelper.createUserInDatabase("Student", STUDENT_EMAIL, Role.STUDENT);

        mockMvc.perform(changeRoleRequest(adminToken, student.getId(), roleJson("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId()))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        assertEquals(Role.STUDENT, roleInDatabase(student.getId()));
    }

    @Test
    void changeRole_withUnknownRole_returnsBadRequest() throws Exception {
        User student = testHelper.createUserInDatabase("Student", STUDENT_EMAIL, Role.STUDENT);

        mockMvc.perform(changeRoleRequest(adminToken, student.getId(), roleJson("MANAGER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for: role."));
    }

    @Test
    void changeRole_withLowerCaseRole_returnsBadRequest() throws Exception {
        User student = testHelper.createUserInDatabase("Student", STUDENT_EMAIL, Role.STUDENT);

        mockMvc.perform(changeRoleRequest(adminToken, student.getId(), roleJson("staff")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for: role."));

        assertEquals(Role.STUDENT, roleInDatabase(student.getId()));
    }

    @Test
    void changeRole_withoutRole_returnsBadRequest() throws Exception {
        User student = testHelper.createUserInDatabase("Student", STUDENT_EMAIL, Role.STUDENT);

        mockMvc.perform(changeRoleRequest(adminToken, student.getId(), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for: role."));
    }

    @Test
    void changeRole_ofUnknownUser_returnsNotFound() throws Exception {
        mockMvc.perform(changeRoleRequest(adminToken, UNKNOWN_ID, roleJson("STAFF")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found."));
    }

    @Test
    void changeRole_ofStudentWithActiveTicket_returnsConflict() throws Exception {
        User student = testHelper.createUserInDatabase("Student", STUDENT_EMAIL, Role.STUDENT);
        ServiceType service = testHelper.createActiveService("Registration", "R");
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, service, 1, TicketStatus.WAITING);

        mockMvc.perform(changeRoleRequest(adminToken, student.getId(), roleJson("STAFF")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cancel or finish this student's ticket first."));

        assertEquals(Role.STUDENT, roleInDatabase(student.getId()));
    }

    @Test
    void changeRole_ofStudentWithFinishedTicket_savesNewRole() throws Exception {
        User student = testHelper.createUserInDatabase("Student", STUDENT_EMAIL, Role.STUDENT);
        ServiceType service = testHelper.createActiveService("Registration", "R");
        testHelper.createTicketForRegisteredStudent(STUDENT_EMAIL, service, 1, TicketStatus.DONE);

        mockMvc.perform(changeRoleRequest(adminToken, student.getId(), roleJson("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"));

        assertEquals(Role.STAFF, roleInDatabase(student.getId()));
    }

    @Test
    void changeRole_ofStudentWithWaitingTicketFromYesterday_savesNewRole() throws Exception {
        User student = testHelper.createUserInDatabase("Student", STUDENT_EMAIL, Role.STUDENT);
        ServiceType service = testHelper.createActiveService("Registration", "R");
        testHelper.createTicketOnDateForRegisteredStudent(
                STUDENT_EMAIL, service, 1, LocalDate.now().minusDays(1), TicketStatus.WAITING);

        mockMvc.perform(changeRoleRequest(adminToken, student.getId(), roleJson("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"));

        assertEquals(Role.STAFF, roleInDatabase(student.getId()));
    }

    private MockHttpServletRequestBuilder listUsersRequest(String token) {
        return get(URL).header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder changeRoleRequest(String token, Long userId, String body) {
        return patch(ROLE_URL, userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String roleJson(String role) {
        return "{\"role\":\"%s\"}".formatted(role);
    }

    private Role roleInDatabase(Long userId) {
        return userRepository.findById(userId).orElseThrow().getRole();
    }
}
