package com.sara.backend.services;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenCounterFinderTest {

    @Autowired
    private OpenCounterFinder openCounterFinder;

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
        deleteAllTestData();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
    }

    @AfterEach
    void deleteAllTestData() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void hasOpenCounter_activeCounterOfActiveService_returnsTrue() {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        testHelper.createCounter("Window 1", registration);

        assertTrue(openCounterFinder.hasOpenCounter(registration.getId()));
    }

    @Test
    void hasOpenCounter_onlyCounterInactive_returnsFalse() {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        deactivate(testHelper.createCounter("Window 1", registration));

        assertFalse(openCounterFinder.hasOpenCounter(registration.getId()));
    }

    @Test
    void hasOpenCounter_serviceWithoutCounters_returnsFalse() {
        ServiceType registration = testHelper.createActiveService("Registration", "R");

        assertFalse(openCounterFinder.hasOpenCounter(registration.getId()));
    }

    @Test
    void hasOpenCounter_activeCounterOfInactiveService_returnsFalse() {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        registration.setActive(false);
        serviceTypeRepository.save(registration);

        assertFalse(openCounterFinder.hasOpenCounter(registration.getId()));
    }

    @Test
    void hasOpenCounter_openCounterOnlyInOtherService_returnsFalse() {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        ServiceType payments = testHelper.createActiveService("Payments", "P");
        testHelper.createCounter("Window 1", payments);

        assertFalse(openCounterFinder.hasOpenCounter(registration.getId()));
    }

    @Test
    void findServiceIdsWithOpenCounter_mixedServices_returnsOnlyServicesWithOpenCounter() {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        ServiceType payments = testHelper.createActiveService("Payments", "P");
        ServiceType library = testHelper.createActiveService("Library", "L");
        testHelper.createActiveService("Advising", "A");
        deactivate(testHelper.createCounter("Window 1", registration));
        testHelper.createCounter("Window 2", payments);
        testHelper.createCounter("Window 3", library);
        library.setActive(false);
        serviceTypeRepository.save(library);

        assertEquals(Set.of(payments.getId()), openCounterFinder.findServiceIdsWithOpenCounter());
    }

    @Test
    void findServiceIdsWithOpenCounter_twoOpenCountersOfSameService_returnsServiceOnce() {
        ServiceType registration = testHelper.createActiveService("Registration", "R");
        testHelper.createCounter("Window 1", registration);
        testHelper.createCounter("Window 2", registration);

        assertEquals(Set.of(registration.getId()), openCounterFinder.findServiceIdsWithOpenCounter());
    }

    private void deactivate(Counter counter) {
        counter.setActive(false);
        counterRepository.save(counter);
    }
}
