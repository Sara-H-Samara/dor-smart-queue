package com.sara.backend.controllers;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.TicketStatus;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin counters")
class CounterControllerTest {

    private static final String URL = "/api/admin/counters";

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

    private ServiceType activeService;

    @BeforeEach
    void resetDatabaseWithSeededAdminAndActiveService() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        activeService = testHelper.createActiveService("Registration", "R");
    }

    private String counterJson(String name, Long serviceId) {
        return "{\"name\":\"%s\",\"serviceId\":%s}".formatted(name, serviceId);
    }

    private long createCounterAsAdmin(String token, String name, Long serviceId) throws Exception {
        String responseBody = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson(name, serviceId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(responseBody, "$.id")).longValue();
    }

    @Test
    @DisplayName("An admin sees all counters with their service")
    void listCounters_asAdmin_returnsCountersWithService() throws Exception {
        String token = testHelper.loginAsAdmin();
        createCounterAsAdmin(token, "Counter 1", activeService.getId());
        createCounterAsAdmin(token, "Counter 2", activeService.getId());

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Counter 1"))
                .andExpect(jsonPath("$[0].serviceId").value(activeService.getId()))
                .andExpect(jsonPath("$[0].serviceName").value("Registration"))
                .andExpect(jsonPath("$[1].name").value("Counter 2"));
    }

    @Test
    @DisplayName("An admin can create a counter for an active service")
    void createCounter_withActiveService_returnsCreatedWithServiceIdAndServiceName() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", activeService.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Counter 1"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.serviceId").value(activeService.getId()))
                .andExpect(jsonPath("$.serviceName").value("Registration"));
    }

    @Test
    @DisplayName("A counter cannot be created without a service")
    void createCounter_withoutServiceId_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Counter 1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A counter cannot be created for a service that does not exist")
    void createCounter_withUnknownServiceId_returnsNotFound() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", 999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A counter cannot be created for an inactive service")
    void createCounter_withInactiveService_returnsBadRequest() throws Exception {
        ServiceType inactiveService = testHelper.createInactiveService("Archive", "A");

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", inactiveService.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("An admin can rename a counter")
    void updateCounter_withNewName_returnsUpdatedName() throws Exception {
        String token = testHelper.loginAsAdmin();
        long id = createCounterAsAdmin(token, "Counter 1", activeService.getId());

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 3", activeService.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Counter 3"));
    }

    @Test
    @DisplayName("An admin can move a counter to another active service")
    void updateCounter_withAnotherActiveService_returnsNewService() throws Exception {
        String token = testHelper.loginAsAdmin();
        long id = createCounterAsAdmin(token, "Counter 1", activeService.getId());
        ServiceType otherService = testHelper.createActiveService("Fees", "F");

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", otherService.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value(otherService.getId()))
                .andExpect(jsonPath("$.serviceName").value("Fees"));
    }

    @Test
    @DisplayName("A counter cannot be updated without a service")
    void updateCounter_withoutServiceId_returnsBadRequest() throws Exception {
        String token = testHelper.loginAsAdmin();
        long id = createCounterAsAdmin(token, "Counter 1", activeService.getId());

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Counter 1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A counter cannot be moved to a service that does not exist")
    void updateCounter_withUnknownServiceId_returnsNotFound() throws Exception {
        String token = testHelper.loginAsAdmin();
        long id = createCounterAsAdmin(token, "Counter 1", activeService.getId());

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", 999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A counter cannot be moved to an inactive service")
    void updateCounter_withDifferentInactiveService_returnsBadRequest() throws Exception {
        String token = testHelper.loginAsAdmin();
        long id = createCounterAsAdmin(token, "Counter 1", activeService.getId());
        ServiceType inactiveService = testHelper.createInactiveService("Archive", "A");

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", inactiveService.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A counter can stay with its service even when that service is inactive")
    void updateCounter_keepingCurrentInactiveService_returnsOk() throws Exception {
        ServiceType inactiveService = testHelper.createInactiveService("Archive", "A");
        long id = testHelper.createCounter("Counter 1", inactiveService).getId();

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 2", inactiveService.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Counter 2"))
                .andExpect(jsonPath("$.serviceId").value(inactiveService.getId()));
    }

    @Test
    @DisplayName("An admin can deactivate a counter and activate it again")
    void setActive_withFalseThenTrue_returnsUpdatedActive() throws Exception {
        String token = testHelper.loginAsAdmin();
        long id = createCounterAsAdmin(token, "Counter 1", activeService.getId());

        mockMvc.perform(patch(URL + "/" + id + "/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch(URL + "/" + id + "/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("A student cannot use any admin counter endpoint")
    void adminEndpoints_asStudent_returnForbidden() throws Exception {
        String token = testHelper.registerStudentAndLogin("layla@example.com");

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", activeService.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(URL + "/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", activeService.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(URL + "/1/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A visitor without a token cannot use any admin counter endpoint")
    void adminEndpoints_withoutToken_returnUnauthorized() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", activeService.getId())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", activeService.getId())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(URL + "/1/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A counter name cannot be used twice")
    void createCounter_withDuplicateName_returnsConflict() throws Exception {
        String token = testHelper.loginAsAdmin();
        createCounterAsAdmin(token, "Counter 1", activeService.getId());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", activeService.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A counter name counts as used even in another letter case")
    void createCounter_withDuplicateNameInDifferentCase_returnsConflict() throws Exception {
        String token = testHelper.loginAsAdmin();
        createCounterAsAdmin(token, "Counter 1", activeService.getId());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("counter 1", activeService.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A counter can be updated with its own name")
    void updateCounter_withSameName_returnsOk() throws Exception {
        String token = testHelper.loginAsAdmin();
        long id = createCounterAsAdmin(token, "Counter 1", activeService.getId());

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", activeService.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Counter 1"));
    }

    @Test
    @DisplayName("A counter cannot take the name of another counter")
    void updateCounter_withAnotherCountersName_returnsConflict() throws Exception {
        String token = testHelper.loginAsAdmin();
        createCounterAsAdmin(token, "Counter 1", activeService.getId());
        long otherId = createCounterAsAdmin(token, "Counter 2", activeService.getId());

        mockMvc.perform(put(URL + "/" + otherId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("counter 1", activeService.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Updating a counter that does not exist gives not found")
    void updateCounter_withUnknownId_returnsNotFound() throws Exception {
        mockMvc.perform(put(URL + "/999999")
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", activeService.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Changing the active state of a counter that does not exist gives not found")
    void setActive_withUnknownId_returnsNotFound() throws Exception {
        mockMvc.perform(patch(URL + "/999999/active")
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A counter name cannot be empty")
    void createCounter_withBlankName_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("   ", activeService.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A counter name cannot be longer than 100 characters")
    void createCounter_withTooLongName_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("a".repeat(101), activeService.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A counter cannot be renamed to an empty name")
    void updateCounter_withBlankName_returnsBadRequest() throws Exception {
        String token = testHelper.loginAsAdmin();
        long id = createCounterAsAdmin(token, "Counter 1", activeService.getId());

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("   ", activeService.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A counter cannot be renamed to a name longer than 100 characters")
    void updateCounter_withTooLongName_returnsBadRequest() throws Exception {
        String token = testHelper.loginAsAdmin();
        long id = createCounterAsAdmin(token, "Counter 1", activeService.getId());

        mockMvc.perform(put(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("a".repeat(101), activeService.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A deactivated counter stays in the admin list")
    void setActive_withFalse_keepsCounterInAdminList() throws Exception {
        String token = testHelper.loginAsAdmin();
        long id = createCounterAsAdmin(token, "Counter 1", activeService.getId());

        mockMvc.perform(patch(URL + "/" + id + "/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].name").value("Counter 1"))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    private void patchActive(String token, long counterId, boolean active, int expectedStatus) throws Exception {
        mockMvc.perform(patch(URL + "/" + counterId + "/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":" + active + "}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("A counter cannot be deactivated while a student is called at it")
    void setActive_withFalseWhileTicketIsCalled_returnsConflictAndKeepsCounterActive() throws Exception {
        Counter counter = testHelper.createCounter("Counter 1", activeService);
        testHelper.registerStudentAndLogin("layla@example.com");
        testHelper.createCalledTicketForRegisteredStudent("layla@example.com", activeService, 1, counter);

        mockMvc.perform(patch(URL + "/" + counter.getId() + "/active")
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Finish the called student first."));

        assertTrue(counterRepository.findById(counter.getId()).orElseThrow().isActive());
    }

    @Test
    @DisplayName("A counter can be deactivated when its called ticket is from an earlier day")
    void updateActive_forCounterWithCalledTicketFromEarlierDay_deactivatesCounter() throws Exception {
        Counter counter = testHelper.createCounter("Counter 1", activeService);
        testHelper.registerStudentAndLogin("layla@example.com");
        testHelper.createCalledTicketOnDateForRegisteredStudent(
                "layla@example.com", activeService, 1, counter, LocalDate.now().minusDays(1));

        patchActive(testHelper.loginAsAdmin(), counter.getId(), false, 200);

        assertFalse(counterRepository.findById(counter.getId()).orElseThrow().isActive());
    }

    @Test
    @DisplayName("A counter can be deactivated when it has no called ticket")
    void setActive_withFalseWithOnlyWaitingAndDoneTickets_returnsOk() throws Exception {
        Counter counter = testHelper.createCounter("Counter 1", activeService);
        testHelper.registerStudentAndLogin("layla@example.com");
        testHelper.createTicketForRegisteredStudent("layla@example.com", activeService, 1, TicketStatus.WAITING);
        testHelper.createTicketForRegisteredStudent("layla@example.com", activeService, 2, TicketStatus.DONE);

        patchActive(testHelper.loginAsAdmin(), counter.getId(), false, 200);
    }

    @Test
    @DisplayName("An inactive counter can be activated even if a student is still called at it")
    void setActive_withTrueWhileTicketIsCalled_returnsOk() throws Exception {
        Counter counter = testHelper.createCounter("Counter 1", activeService);
        counter.setActive(false);
        counterRepository.save(counter);
        testHelper.registerStudentAndLogin("layla@example.com");
        testHelper.createCalledTicketForRegisteredStudent("layla@example.com", activeService, 1, counter);

        patchActive(testHelper.loginAsAdmin(), counter.getId(), true, 200);
    }

    @Test
    @DisplayName("A counter cannot change its service while a student is called at it")
    void updateCounter_withAnotherServiceWhileTicketIsCalled_returnsConflictAndKeepsService() throws Exception {
        Counter counter = testHelper.createCounter("Counter 1", activeService);
        ServiceType otherService = testHelper.createActiveService("Fees", "F");
        testHelper.registerStudentAndLogin("layla@example.com");
        testHelper.createCalledTicketForRegisteredStudent("layla@example.com", activeService, 1, counter);

        mockMvc.perform(put(URL + "/" + counter.getId())
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", otherService.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Finish the called student first."));

        Counter unchangedCounter = counterRepository.findAll().get(0);
        assertEquals(activeService.getId(), unchangedCounter.getServiceType().getId());
    }

    @Test
    @DisplayName("A counter can be renamed while a student is called at it")
    void updateCounter_withNewNameWhileTicketIsCalled_returnsOk() throws Exception {
        Counter counter = testHelper.createCounter("Counter 1", activeService);
        testHelper.registerStudentAndLogin("layla@example.com");
        testHelper.createCalledTicketForRegisteredStudent("layla@example.com", activeService, 1, counter);

        mockMvc.perform(put(URL + "/" + counter.getId())
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 2", activeService.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Counter 2"));
    }
}
