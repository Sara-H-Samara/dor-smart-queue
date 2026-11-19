package com.sara.backend.controllers;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.ServiceType;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Counter list for staff")
class StaffCounterControllerTest {

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
    private String staffToken;

    @BeforeEach
    void resetDatabaseWithStaffAndService() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        registration = testHelper.createActiveService("Registration", "R");
        staffToken = testHelper.loginAsNewStaff();
    }

    @Test
    @DisplayName("Staff see the counters sorted by name")
    void listAvailableCounters_asStaff_returnsCountersOrderedByName() throws Exception {
        testHelper.createCounter("Desk 2", registration);
        testHelper.createCounter("Desk 1", registration);

        testHelper.listCounters(staffToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Desk 1"))
                .andExpect(jsonPath("$[1].name").value("Desk 2"));
    }

    @Test
    @DisplayName("Each counter shows only its id, name and service")
    void listAvailableCounters_asStaff_returnsOnlyDocumentedFields() throws Exception {
        Counter deskOne = testHelper.createCounter("Desk 1", registration);

        testHelper.listCounters(staffToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(deskOne.getId()))
                .andExpect(jsonPath("$[0].name").value("Desk 1"))
                .andExpect(jsonPath("$[0].serviceId").value(registration.getId()))
                .andExpect(jsonPath("$[0].serviceName").value("Registration"))
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    @Test
    @DisplayName("An admin can also see the counter list")
    void listAvailableCounters_asAdmin_returnsCounters() throws Exception {
        testHelper.createCounter("Desk 1", registration);

        testHelper.listCounters(testHelper.loginAsAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("An inactive counter is not in the list")
    void listAvailableCounters_withInactiveCounter_omitsIt() throws Exception {
        Counter inactiveDesk = testHelper.createCounter("Desk 1", registration);
        inactiveDesk.setActive(false);
        counterRepository.save(inactiveDesk);
        testHelper.createCounter("Desk 2", registration);

        testHelper.listCounters(staffToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Desk 2"));
    }

    @Test
    @DisplayName("A counter of an inactive service is not in the list")
    void listAvailableCounters_withInactiveService_omitsIt() throws Exception {
        ServiceType inactiveService = testHelper.createInactiveService("Archive", "A");
        testHelper.createCounter("Desk 1", inactiveService);
        testHelper.createCounter("Desk 2", registration);

        testHelper.listCounters(staffToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Desk 2"));
    }

    @Test
    @DisplayName("The list is empty when there are no counters")
    void listAvailableCounters_withNoCounters_returnsEmptyList() throws Exception {
        testHelper.listCounters(staffToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("A student cannot see the counter list")
    void listAvailableCounters_asStudent_returnsForbidden() throws Exception {
        String studentToken = testHelper.registerStudentAndLogin("a@example.com");

        testHelper.listCounters(studentToken).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A visitor without a token cannot see the counter list")
    void listAvailableCounters_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/counters")).andExpect(status().isUnauthorized());
    }
}
