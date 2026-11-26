package com.sara.backend.controllers;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Role;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.Callable;

import static com.sara.backend.support.SimultaneousRequests.runAtTheSameTime;
import static com.sara.backend.support.SimultaneousRequests.statusesOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Role changes at the same time")
class UserRoleConcurrencyTest {

    private static final int ROUNDS = 20;
    private static final int OK = 200;
    private static final int UNAUTHORIZED = 401;
    private static final String FIRST_ADMIN_EMAIL = "admin@dor.com";
    private static final String SECOND_ADMIN_EMAIL = "second-admin@example.com";

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
    private User firstAdmin;
    private User secondAdmin;
    private String firstAdminToken;
    private String secondAdminToken;

    @BeforeEach
    void resetDatabaseWithTwoAdmins() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        firstAdmin = userRepository.findByEmail(FIRST_ADMIN_EMAIL).orElseThrow();
        secondAdmin = testHelper.createUserInDatabase("Second Admin", SECOND_ADMIN_EMAIL, Role.ADMIN);
        firstAdminToken = testHelper.loginAsAdmin();
        secondAdminToken = testHelper.loginAndGetToken(SECOND_ADMIN_EMAIL, "secret123");
    }

    @Test
    @DisplayName("Two admins who demote each other at the same time leave one admin")
    void changeRole_whenTwoAdminsDemoteEachOtherAtOnce_keepsOneAdmin() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            testHelper.updateRoleInDatabase(FIRST_ADMIN_EMAIL, Role.ADMIN);
            testHelper.updateRoleInDatabase(SECOND_ADMIN_EMAIL, Role.ADMIN);

            List<MockHttpServletResponse> responses = runAtTheSameTime(List.of(
                    demoteRequest(firstAdminToken, secondAdmin),
                    demoteRequest(secondAdminToken, firstAdmin)));

            assertEquals(List.of(OK, UNAUTHORIZED), statusesOf(responses));
            assertEquals(1, userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.ADMIN).count());
        }
    }

    private Callable<MockHttpServletResponse> demoteRequest(String token, User target) {
        return () -> mockMvc.perform(patch("/api/admin/users/{id}/role", target.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"STAFF\"}"))
                .andReturn().getResponse();
    }
}
