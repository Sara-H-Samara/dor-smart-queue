package com.sara.backend.repositories;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.support.TicketTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Finished tickets query")
class FinishedTicketQueryTest {

    private static final String STUDENT_EMAIL = "a@example.com";
    private static final Pageable FIRST_TEN = PageRequest.of(0, 10);

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
    private Counter deskOne;

    @BeforeEach
    void resetDatabaseWithOneStudentAndCounter() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        registration = testHelper.createActiveService("Registration", "R");
        deskOne = testHelper.createCounter("Desk 1", registration);
        testHelper.registerStudentAndLogin(STUDENT_EMAIL);
    }

    @AfterEach
    void deleteAllTickets() {
        ticketRepository.deleteAll();
    }

    private Ticket finishedTicket(ServiceType service, int number, Duration serviceDuration) {
        return testHelper.createFinishedTicketForRegisteredStudent(
                STUDENT_EMAIL, service, number, deskOne, serviceDuration);
    }

    private List<FinishedTicketRow> findRowsOfRegistration(Pageable pageable) {
        return ticketRepository.findRecentFinishedTicketRowsOfService(
                registration.getId(), LocalDate.now(), pageable);
    }

    @Test
    void findRecentFinishedTicketRowsOfService_withDoneTickets_returnsRowsWithServiceDuration() {
        finishedTicket(registration, 1, Duration.ofMinutes(7));

        List<FinishedTicketRow> rows = findRowsOfRegistration(FIRST_TEN);

        assertEquals(1, rows.size());
        assertEquals(Duration.ofMinutes(7), rows.get(0).serviceDuration());
    }

    @Test
    void findRecentFinishedTicketRowsOfService_withSeveralDoneTickets_returnsNewestFinishedFirst() {
        LocalDateTime now = LocalDateTime.now();
        testHelper.createFinishedTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 1, deskOne, now.minusMinutes(30), now.minusMinutes(20));
        testHelper.createFinishedTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 2, deskOne, now.minusMinutes(20), now.minusMinutes(2));
        testHelper.createFinishedTicketForRegisteredStudent(
                STUDENT_EMAIL, registration, 3, deskOne, now.minusMinutes(10), now.minusMinutes(5));

        List<Duration> durations = findRowsOfRegistration(FIRST_TEN).stream()
                .map(FinishedTicketRow::serviceDuration)
                .toList();

        assertEquals(List.of(Duration.ofMinutes(18), Duration.ofMinutes(5), Duration.ofMinutes(10)), durations);
    }

    @Test
    void findRecentFinishedTicketRowsOfService_withCancelledTicket_ignoresIt() {
        Ticket cancelled = finishedTicket(registration, 1, Duration.ofMinutes(7));
        cancelled.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(cancelled);

        assertTrue(findRowsOfRegistration(FIRST_TEN).isEmpty());
    }

    @Test
    void findRecentFinishedTicketRowsOfService_withNoShowTicket_ignoresIt() {
        Ticket noShow = finishedTicket(registration, 1, Duration.ofMinutes(7));
        noShow.setStatus(TicketStatus.NO_SHOW);
        ticketRepository.save(noShow);

        assertTrue(findRowsOfRegistration(FIRST_TEN).isEmpty());
    }

    @Test
    void findRecentFinishedTicketRowsOfService_withCalledTicket_ignoresIt() {
        Ticket called = finishedTicket(registration, 1, Duration.ofMinutes(7));
        called.setStatus(TicketStatus.CALLED);
        ticketRepository.save(called);

        assertTrue(findRowsOfRegistration(FIRST_TEN).isEmpty());
    }

    @Test
    void findRecentFinishedTicketRowsOfService_withTicketOfOtherService_ignoresIt() {
        ServiceType documents = testHelper.createActiveService("Documents", "D");
        finishedTicket(documents, 1, Duration.ofMinutes(7));

        assertTrue(findRowsOfRegistration(FIRST_TEN).isEmpty());
    }

    @Test
    void findRecentFinishedTicketRowsOfService_withTicketOfEarlierDay_ignoresIt() {
        Ticket yesterday = finishedTicket(registration, 1, Duration.ofMinutes(7));
        yesterday.setTicketDate(LocalDate.now().minusDays(1));
        ticketRepository.save(yesterday);

        assertTrue(findRowsOfRegistration(FIRST_TEN).isEmpty());
    }

    @Test
    void findRecentFinishedTicketRowsOfService_withDoneTicketWithoutFinishedAt_ignoresIt() {
        Ticket withoutFinishedAt = finishedTicket(registration, 1, Duration.ofMinutes(7));
        withoutFinishedAt.setFinishedAt(null);
        ticketRepository.save(withoutFinishedAt);

        assertTrue(findRowsOfRegistration(FIRST_TEN).isEmpty());
    }

    @Test
    void findRecentFinishedTicketRowsOfService_withDoneTicketWithoutCalledAt_ignoresIt() {
        Ticket withoutCalledAt = finishedTicket(registration, 1, Duration.ofMinutes(7));
        withoutCalledAt.setCalledAt(null);
        ticketRepository.save(withoutCalledAt);

        assertTrue(findRowsOfRegistration(FIRST_TEN).isEmpty());
    }

    @Test
    void findRecentFinishedTicketRowsOfService_withMoreTicketsThanLimit_returnsOnlyTheLatest() {
        LocalDateTime now = LocalDateTime.now();
        for (int number = 1; number <= 5; number++) {
            LocalDateTime finishedAt = now.minusMinutes(60 - number);
            testHelper.createFinishedTicketForRegisteredStudent(
                    STUDENT_EMAIL, registration, number, deskOne, finishedAt.minusMinutes(number), finishedAt);
        }

        List<Duration> durations = findRowsOfRegistration(PageRequest.of(0, 2)).stream()
                .map(FinishedTicketRow::serviceDuration)
                .toList();

        assertEquals(List.of(Duration.ofMinutes(5), Duration.ofMinutes(4)), durations);
    }
}
