package com.sara.backend.support;

import com.jayway.jsonpath.JsonPath;
import com.sara.backend.entity.Role;
import com.sara.backend.entity.ServiceType;
import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;
import com.sara.backend.entity.User;
import com.sara.backend.repository.ServiceTypeRepository;
import com.sara.backend.repository.TicketRepository;
import com.sara.backend.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TicketTestHelper {

    private static final String STUDENT_PASSWORD = "secret123";

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public TicketTestHelper(
            MockMvc mockMvc,
            UserRepository userRepository,
            ServiceTypeRepository serviceTypeRepository,
            TicketRepository ticketRepository,
            PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.ticketRepository = ticketRepository;
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
}
