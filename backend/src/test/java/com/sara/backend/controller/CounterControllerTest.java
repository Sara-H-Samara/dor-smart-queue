package com.sara.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.config.DataSeeder;
import com.sara.backend.entity.Counter;
import com.sara.backend.entity.ServiceType;
import com.sara.backend.entity.TicketStatus;
import com.sara.backend.repository.CounterRepository;
import com.sara.backend.repository.ServiceTypeRepository;
import com.sara.backend.repository.TicketRepository;
import com.sara.backend.repository.UserRepository;
import com.sara.backend.support.TicketTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void createCounter_withoutServiceId_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Counter 1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCounter_withUnknownServiceId_returnsNotFound() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", 999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCounter_withInactiveService_returnsBadRequest() throws Exception {
        ServiceType inactiveService = testHelper.createInactiveService("Archive", "A");

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", inactiveService.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
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
    void updateCounter_withUnknownId_returnsNotFound() throws Exception {
        mockMvc.perform(put(URL + "/999999")
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("Counter 1", activeService.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void setActive_withUnknownId_returnsNotFound() throws Exception {
        mockMvc.perform(patch(URL + "/999999/active")
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCounter_withBlankName_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("   ", activeService.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCounter_withTooLongName_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + testHelper.loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(counterJson("a".repeat(101), activeService.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
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
    void setActive_withFalseWithOnlyWaitingAndDoneTickets_returnsOk() throws Exception {
        Counter counter = testHelper.createCounter("Counter 1", activeService);
        testHelper.registerStudentAndLogin("layla@example.com");
        testHelper.createTicketForRegisteredStudent("layla@example.com", activeService, 1, TicketStatus.WAITING);
        testHelper.createTicketForRegisteredStudent("layla@example.com", activeService, 2, TicketStatus.DONE);

        patchActive(testHelper.loginAsAdmin(), counter.getId(), false, 200);
    }

    @Test
    void setActive_withTrueWhileTicketIsCalled_returnsOk() throws Exception {
        Counter counter = testHelper.createCounter("Counter 1", activeService);
        counter.setActive(false);
        counterRepository.save(counter);
        testHelper.registerStudentAndLogin("layla@example.com");
        testHelper.createCalledTicketForRegisteredStudent("layla@example.com", activeService, 1, counter);

        patchActive(testHelper.loginAsAdmin(), counter.getId(), true, 200);
    }

    @Test
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
