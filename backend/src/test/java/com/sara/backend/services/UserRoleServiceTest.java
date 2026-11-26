package com.sara.backend.services;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.dtos.requests.RoleChangeRequest;
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
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Role changes in the service")
class UserRoleServiceTest {

    private static final String DEMOTED_CALLER_EMAIL = "staff@dor.com";

    @Autowired
    private UserRoleService userRoleService;

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
    void resetDatabaseWithSeededUsers() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
    }

    @Test
    void changeRole_whenCallerWasDemotedMeanwhile_returnsUnauthorized() {
        User secondAdmin = testHelper.createUserInDatabase("Second Admin", "second-admin@example.com", Role.ADMIN);
        testHelper.createUserInDatabase("Third Admin", "third-admin@example.com", Role.ADMIN);

        assertThrows(InsufficientAuthenticationException.class, () -> userRoleService.changeRole(
                secondAdmin.getId(), new RoleChangeRequest("STAFF"), DEMOTED_CALLER_EMAIL));

        assertEquals(Role.ADMIN, userRepository.findById(secondAdmin.getId()).orElseThrow().getRole());
    }
}
