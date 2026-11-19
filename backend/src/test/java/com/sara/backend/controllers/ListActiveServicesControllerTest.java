package com.sara.backend.controllers;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.ServiceType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Service list for students")
class ListActiveServicesControllerTest {

    private static final String SERVICES_URL = "/api/services";

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
    @DisplayName("A student sees only the active services, sorted by name")
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
    @DisplayName("A service with an active counter is shown as open")
    void listActiveServices_serviceWithActiveCounter_hasOpenCounterTrue() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        String token = testHelper.registerStudentAndLogin("a@example.com");

        mockMvc.perform(get(SERVICES_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Registration"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].hasOpenCounter").value(true));
    }

    @Test
    @DisplayName("A service whose only counter is inactive is shown as closed")
    void listActiveServices_serviceWithOnlyInactiveCounter_hasOpenCounterFalse() throws Exception {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        Counter window = testHelper.createCounter("Window 1", registration);
        window.setActive(false);
        counterRepository.save(window);
        String token = testHelper.registerStudentAndLogin("a@example.com");

        mockMvc.perform(get(SERVICES_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Registration"))
                .andExpect(jsonPath("$[0].hasOpenCounter").value(false));
    }

    @Test
    @DisplayName("A service without counters is shown as closed")
    void listActiveServices_serviceWithoutCounters_hasOpenCounterFalse() throws Exception {
        testHelper.createActiveService("Registration", "R");
        ServiceType documents = testHelper.createActiveService("Documents", "D");
        testHelper.createCounter("Window 1", documents);
        String token = testHelper.registerStudentAndLogin("a@example.com");

        mockMvc.perform(get(SERVICES_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Documents"))
                .andExpect(jsonPath("$[0].hasOpenCounter").value(true))
                .andExpect(jsonPath("$[1].name").value("Registration"))
                .andExpect(jsonPath("$[1].hasOpenCounter").value(false));
    }

    @Test
    @DisplayName("The list is empty when there are no active services")
    void listActiveServices_withNoActiveServices_returnsEmptyList() throws Exception {
        String token = testHelper.registerStudentAndLogin("a@example.com");

        mockMvc.perform(get(SERVICES_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Staff cannot use the student service list")
    void listActiveServices_asStaff_returnsForbidden() throws Exception {
        String token = testHelper.loginAsNewStaff();

        mockMvc.perform(get(SERVICES_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An admin cannot use the student service list")
    void listActiveServices_asAdmin_returnsForbidden() throws Exception {
        String token = testHelper.loginAsAdmin();

        mockMvc.perform(get(SERVICES_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A visitor without a token cannot see the service list")
    void listActiveServices_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(SERVICES_URL))
                .andExpect(status().isUnauthorized());
    }
}
