package com.sara.backend.controllers;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.Role;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.entities.User;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerTest {

    private static final String URL = "/api/tickets";

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
    void deleteAllTestData() {
        // Tickets first: they reference users and services.
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
        // Other test classes do not clear tickets, so leave none behind.
        ticketRepository.deleteAll();
    }

    // ---------- Helpers ----------

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String adminToken() throws Exception {
        return loginAndGetToken("admin@dor.com", "admin12345");
    }

    /** Creates a staff user directly (so the test does not depend on the seeded password). */
    private String staffToken() throws Exception {
        User staff = new User();
        staff.setFullName("Test Staff");
        staff.setEmail("test-staff@example.com");
        staff.setPasswordHash(passwordEncoder.encode("staff12345"));
        staff.setRole(Role.STAFF);
        userRepository.save(staff);
        return loginAndGetToken("test-staff@example.com", "staff12345");
    }

    private String registerStudentAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Student","email":"%s","password":"secret123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        return loginAndGetToken(email, "secret123");
    }

    private ServiceType createService(String name, String code, boolean active) {
        ServiceType service = new ServiceType();
        service.setName(name);
        service.setCode(code);
        service.setActive(active);
        return serviceTypeRepository.save(service);
    }

    private String body(Long serviceId) {
        return "{\"serviceId\":%d}".formatted(serviceId);
    }

    private org.springframework.test.web.servlet.ResultActions take(String token, Long serviceId) throws Exception {
        return mockMvc.perform(post(URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(serviceId)));
    }

    // ---------- Taking tickets ----------

    @Test
    void takeTicket_firstStudent_returnsCreatedWithFirstCode() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        testHelper.createCounter("Window 1", registration);

        take(registerStudentAndLogin("a@example.com"), registration.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ticketCode").value("R-001"))
                .andExpect(jsonPath("$.serviceId").value(registration.getId()))
                .andExpect(jsonPath("$.serviceName").value("Registration"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void takeTicket_secondStudentSameService_returnsNextNumber() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        testHelper.createCounter("Window 1", registration);

        take(registerStudentAndLogin("a@example.com"), registration.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("R-001"));
        take(registerStudentAndLogin("b@example.com"), registration.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("R-002"));
    }

    @Test
    void takeTicket_differentService_restartsNumberingAtOne() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        ServiceType documents = createService("Documents", "D", true);
        testHelper.createCounter("Window 1", registration);
        testHelper.createCounter("Window 2", documents);

        take(registerStudentAndLogin("a@example.com"), registration.getId())
                .andExpect(jsonPath("$.ticketCode").value("R-001"));
        take(registerStudentAndLogin("b@example.com"), documents.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("D-001"));
    }

    // ---------- One active ticket per student ----------

    @Test
    void takeTicket_whileStudentHasWaitingTicket_returnsConflict() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        ServiceType documents = createService("Documents", "D", true);
        testHelper.createCounter("Window 1", registration);
        testHelper.createCounter("Window 2", documents);
        String token = registerStudentAndLogin("a@example.com");

        take(token, registration.getId()).andExpect(status().isCreated());

        take(token, documents.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have an active ticket."));
    }

    @Test
    void takeTicket_afterServed_returnsCreated() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        testHelper.createCounter("Window 1", registration);
        String token = registerStudentAndLogin("a@example.com");

        take(token, registration.getId()).andExpect(status().isCreated());
        Ticket first = ticketRepository.findAll().get(0);
        first.setStatus(TicketStatus.DONE);
        ticketRepository.save(first);

        take(token, registration.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("R-002"));
    }

    @Test
    void takeTicket_afterCancelled_returnsCreated() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        testHelper.createCounter("Window 1", registration);
        String token = registerStudentAndLogin("a@example.com");

        take(token, registration.getId()).andExpect(status().isCreated());
        Ticket first = ticketRepository.findAll().get(0);
        first.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(first);

        take(token, registration.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("R-002"));
    }

    @Test
    void takeTicket_withWaitingTicketFromYesterday_returnsCreatedAndKeepsOldTicketWaiting() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        testHelper.createCounter("Window 1", registration);
        String token = registerStudentAndLogin("a@example.com");
        Ticket yesterdayTicket = testHelper.createTicketOnDateForRegisteredStudent(
                "a@example.com", registration, 1, LocalDate.now().minusDays(1), TicketStatus.WAITING);

        take(token, registration.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("R-001"));

        assertEquals(TicketStatus.WAITING, ticketRepository.findById(yesterdayTicket.getId()).orElseThrow().getStatus());
    }

    @Test
    void takeTicket_withCalledTicketFromYesterday_returnsCreatedAndKeepsOldTicketCalled() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        testHelper.createCounter("Window 1", registration);
        String token = registerStudentAndLogin("a@example.com");
        Ticket yesterdayTicket = testHelper.createTicketOnDateForRegisteredStudent(
                "a@example.com", registration, 1, LocalDate.now().minusDays(1), TicketStatus.CALLED);

        take(token, registration.getId()).andExpect(status().isCreated());

        assertEquals(TicketStatus.CALLED, ticketRepository.findById(yesterdayTicket.getId()).orElseThrow().getStatus());
    }

    // ---------- Service problems and validation ----------

    @Test
    void takeTicket_inactiveService_returnsBadRequest() throws Exception {
        ServiceType closed = createService("Closed", "C", false);

        take(registerStudentAndLogin("a@example.com"), closed.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This service is not available."));
    }

    @Test
    void takeTicket_serviceWithoutCounter_returnsConflict() throws Exception {
        ServiceType registration = createService("Registration", "R", true);

        take(registerStudentAndLogin("a@example.com"), registration.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No counter is open for Registration right now."));

        assertEquals(0, ticketRepository.count());
    }

    @Test
    void takeTicket_onlyCounterInactive_returnsConflictWithServiceName() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        Counter window = testHelper.createCounter("Window 1", registration);
        window.setActive(false);
        counterRepository.save(window);

        take(registerStudentAndLogin("a@example.com"), registration.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No counter is open for Registration right now."));
    }

    @Test
    void takeTicket_afterCounterReactivated_returnsCreated() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        Counter window = testHelper.createCounter("Window 1", registration);
        window.setActive(false);
        counterRepository.save(window);
        String token = registerStudentAndLogin("a@example.com");
        take(token, registration.getId()).andExpect(status().isConflict());

        window.setActive(true);
        counterRepository.save(window);

        take(token, registration.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("R-001"));
    }

    @Test
    void takeTicket_unknownService_returnsNotFound() throws Exception {
        take(registerStudentAndLogin("a@example.com"), 999999L)
                .andExpect(status().isNotFound());
    }

    @Test
    void takeTicket_missingServiceId_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + registerStudentAndLogin("a@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Access control ----------

    @Test
    void takeTicket_withoutToken_returnsUnauthorized() throws Exception {
        ServiceType registration = createService("Registration", "R", true);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(registration.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void takeTicket_asStaffOrAdmin_returnsForbidden() throws Exception {
        ServiceType registration = createService("Registration", "R", true);

        take(staffToken(), registration.getId()).andExpect(status().isForbidden());
        take(adminToken(), registration.getId()).andExpect(status().isForbidden());
    }

    // ---------- Concurrency ----------

    @Test
    void takeTicket_concurrentStudents_returnsUniqueConsecutiveNumbers() throws Exception {
        int students = 5;
        ServiceType registration = createService("Registration", "R", true);
        testHelper.createCounter("Window 1", registration);
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < students; i++) {
            tokens.add(registerStudentAndLogin("student" + i + "@example.com"));
        }

        CountDownLatch ready = new CountDownLatch(students);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(students);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (String token : tokens) {
                Callable<String> task = () -> {
                    ready.countDown();
                    start.await();
                    var response = take(token, registration.getId()).andReturn().getResponse();
                    return response.getStatus() + ":" + response.getContentAsString();
                };
                futures.add(pool.submit(task));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS), "workers did not start");
            start.countDown();

            Set<Integer> numbers = new HashSet<>();
            for (Future<String> future : futures) {
                String result = future.get(60, TimeUnit.SECONDS);
                int separator = result.indexOf(':');
                assertEquals("201", result.substring(0, separator), result);
                String code = JsonPath.read(result.substring(separator + 1), "$.ticketCode");
                numbers.add(Integer.parseInt(code.substring(code.indexOf('-') + 1)));
            }

            Set<Integer> expected = new HashSet<>();
            for (int i = 1; i <= students; i++) {
                expected.add(i);
            }
            assertEquals(expected, numbers);
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "workers did not finish");
        }
    }
}
