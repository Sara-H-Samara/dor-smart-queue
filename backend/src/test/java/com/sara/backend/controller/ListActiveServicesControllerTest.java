package com.sara.backend.controller;

import com.sara.backend.config.DataSeeder;
import com.sara.backend.entity.ServiceType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListActiveServicesControllerTest {

    private static final String SERVICES_URL = "/api/services";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, passwordEncoder);
    }

    @AfterEach
    void deleteAllTickets() {
        ticketRepository.deleteAll();
    }

    @Test
    void listActiveServices_asStudent_returnsOnlyActiveServicesSortedByName() throws Exception {
        testHelper.createActiveService("Registration", "R");
        testHelper.createActiveService("Documents", "D");
        ServiceType inactiveService = testHelper.createActiveService("Archive", "A");
        inactiveService.setActive(false);
        serviceTypeRepository.save(inactiveService);
        String token = testHelper.registerStudentAndLogin("a@example.com");

        mockMvc.perform(get(SERVICES_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Documents"))
                .andExpect(jsonPath("$[0].code").value("D"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].name").value("Registration"));
    }

    @Test
    void listActiveServices_withNoActiveServices_returnsEmptyList() throws Exception {
        String token = testHelper.registerStudentAndLogin("a@example.com");

        mockMvc.perform(get(SERVICES_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listActiveServices_asStaff_returnsForbidden() throws Exception {
        String token = testHelper.loginAsNewStaff();

        mockMvc.perform(get(SERVICES_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listActiveServices_asAdmin_returnsForbidden() throws Exception {
        String token = testHelper.loginAsAdmin();

        mockMvc.perform(get(SERVICES_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listActiveServices_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(SERVICES_URL))
                .andExpect(status().isUnauthorized());
    }
}
