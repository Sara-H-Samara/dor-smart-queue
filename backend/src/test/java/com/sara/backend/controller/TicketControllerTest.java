package com.sara.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.config.DataSeeder;
import com.sara.backend.entity.Role;
import com.sara.backend.entity.ServiceType;
import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;
import com.sara.backend.entity.User;
import com.sara.backend.repository.ServiceTypeRepository;
import com.sara.backend.repository.TicketRepository;
import com.sara.backend.repository.UserRepository;
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
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataSeeder dataSeeder;

    @BeforeEach
    void deleteAllTestData() {
        // Tickets first: they reference users and services.
        ticketRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
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
        String token = registerStudentAndLogin("a@example.com");

        take(token, registration.getId()).andExpect(status().isCreated());

        take(token, documents.getId())
                .andExpect(status().isConflict())
                .andExpect(status().reason("You already have an active ticket."));
    }

    @Test
    void takeTicket_afterServed_returnsCreated() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        String token = registerStudentAndLogin("a@example.com");

        take(token, registration.getId()).andExpect(status().isCreated());
        Ticket first = ticketRepository.findAll().get(0);
        first.setStatus(TicketStatus.SERVED);
        ticketRepository.save(first);

        take(token, registration.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("R-002"));
    }

    @Test
    void takeTicket_afterCancelled_returnsCreated() throws Exception {
        ServiceType registration = createService("Registration", "R", true);
        String token = registerStudentAndLogin("a@example.com");

        take(token, registration.getId()).andExpect(status().isCreated());
        Ticket first = ticketRepository.findAll().get(0);
        first.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(first);

        take(token, registration.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("R-002"));
    }

    // ---------- Service problems and validation ----------

    @Test
    void takeTicket_inactiveService_returnsBadRequest() throws Exception {
        ServiceType closed = createService("Closed", "C", false);

        take(registerStudentAndLogin("a@example.com"), closed.getId())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("This service is not available."));
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
