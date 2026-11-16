package com.sara.backend.support;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.entity.Counter;
import com.sara.backend.entity.Role;
import com.sara.backend.entity.ServiceType;
import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;
import com.sara.backend.entity.User;
import com.sara.backend.repository.CounterRepository;
import com.sara.backend.repository.ServiceTypeRepository;
import com.sara.backend.repository.TicketRepository;
import com.sara.backend.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TicketTestHelper {

    private static final String STUDENT_PASSWORD = "secret123";

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final TicketRepository ticketRepository;
    private final CounterRepository counterRepository;
    private final PasswordEncoder passwordEncoder;

    public TicketTestHelper(
            MockMvc mockMvc,
            UserRepository userRepository,
            ServiceTypeRepository serviceTypeRepository,
            TicketRepository ticketRepository,
            CounterRepository counterRepository,
            PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.ticketRepository = ticketRepository;
        this.counterRepository = counterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String loginAndGetToken(String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(responseBody, "$.token");
    }

    public String registerStudentAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Student","email":"%s","password":"%s"}
                                """.formatted(email, STUDENT_PASSWORD)))
                .andExpect(status().isCreated());
        return loginAndGetToken(email, STUDENT_PASSWORD);
    }

    public String loginAsAdmin() throws Exception {
        return loginAndGetToken("admin@dor.com", "admin12345");
    }

    public String loginAsNewStaff() throws Exception {
        User staff = new User();
        staff.setFullName("Test Staff");
        staff.setEmail("test-staff@example.com");
        staff.setPasswordHash(passwordEncoder.encode("staff12345"));
        staff.setRole(Role.STAFF);
        userRepository.save(staff);
        return loginAndGetToken("test-staff@example.com", "staff12345");
    }

    public ServiceType createActiveService(String name, String code) {
        ServiceType service = new ServiceType();
        service.setName(name);
        service.setCode(code);
        service.setActive(true);
        return serviceTypeRepository.save(service);
    }

    public ServiceType createInactiveService(String name, String code) {
        ServiceType service = new ServiceType();
        service.setName(name);
        service.setCode(code);
        service.setActive(false);
        return serviceTypeRepository.save(service);
    }

    public Counter createCounter(String name, ServiceType service) {
        Counter counter = new Counter();
        counter.setName(name);
        counter.setServiceType(service);
        return counterRepository.save(counter);
    }

    public Ticket createTicketForRegisteredStudent(
            String email, ServiceType service, int number, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setStudent(userRepository.findByEmail(email).orElseThrow());
        ticket.setServiceType(service);
        ticket.setNumber(number);
        ticket.setTicketDate(LocalDate.now());
        ticket.setStatus(status);
        return ticketRepository.save(ticket);
    }

    public Ticket createCalledTicketForRegisteredStudent(
            String email, ServiceType service, int number, Counter counter) {
        Ticket ticket = new Ticket();
        ticket.setStudent(userRepository.findByEmail(email).orElseThrow());
        ticket.setServiceType(service);
        ticket.setNumber(number);
        ticket.setTicketDate(LocalDate.now());
        ticket.setStatus(TicketStatus.CALLED);
        ticket.setCounter(counter);
        ticket.setCalledAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public Ticket createTicketOnDateForRegisteredStudent(
            String email, ServiceType service, int number, LocalDate ticketDate, TicketStatus status) {
        Ticket ticket = createTicketForRegisteredStudent(email, service, number, status);
        ticket.setTicketDate(ticketDate);
        return ticketRepository.save(ticket);
    }

    public ResultActions callNext(String token, Long counterId) throws Exception {
        return mockMvc.perform(post("/api/counters/{counterId}/call-next", counterId)
                .header("Authorization", "Bearer " + token));
    }

    public ResultActions listCounters(String token) throws Exception {
        return mockMvc.perform(get("/api/counters").header("Authorization", "Bearer " + token));
    }

    public ResultActions getCurrentStudent(String token, Long counterId) throws Exception {
        return mockMvc.perform(get("/api/counters/{counterId}/current", counterId)
                .header("Authorization", "Bearer " + token));
    }

    public ResultActions markDone(String token, Long ticketId) throws Exception {
        return mockMvc.perform(post("/api/tickets/{ticketId}/done", ticketId)
                .header("Authorization", "Bearer " + token));
    }

    public ResultActions markNoShow(String token, Long ticketId) throws Exception {
        return mockMvc.perform(post("/api/tickets/{ticketId}/no-show", ticketId)
                .header("Authorization", "Bearer " + token));
    }
}
