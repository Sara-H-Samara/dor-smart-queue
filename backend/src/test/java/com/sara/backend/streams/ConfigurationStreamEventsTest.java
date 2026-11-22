package com.sara.backend.streams;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.support.OpenStreams;
import com.sara.backend.support.StreamTestHelper;
import com.sara.backend.support.TicketTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin changes on a real server")
class ConfigurationStreamEventsTest {

    private static final String STUDENT_EMAIL = "student@example.com";
    private static final long UNKNOWN_ID = 999_999L;

    @LocalServerPort
    private int port;

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

    @Autowired
    private QueueStreamRegistry queueStreamRegistry;

    @Autowired
    private QueueChangeAnnouncer queueChangeAnnouncer;

    private TicketTestHelper ticketTestHelper;
    private StreamTestHelper streamTestHelper;
    private ServiceType registration;
    private ServiceType inactiveArchive;
    private Counter deskOne;
    private Counter inactiveDeskTwo;
    private String studentToken;
    private String staffToken;
    private String adminToken;

    @BeforeEach
    void resetDatabaseAndLogInAllRoles() throws Exception {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        serviceTypeRepository.deleteAll();
        userRepository.deleteAll();
        dataSeeder.run();
        streamTestHelper = new StreamTestHelper(queueStreamRegistry);
        streamTestHelper.removeAllEmitters();
        ticketTestHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        registration = ticketTestHelper.createActiveService("Registration", "R");
        inactiveArchive = ticketTestHelper.createInactiveService("Archive", "A");
        deskOne = ticketTestHelper.createCounter("Desk 1", registration);
        inactiveDeskTwo = ticketTestHelper.createCounter("Desk 2", registration);
        inactiveDeskTwo.setActive(false);
        counterRepository.save(inactiveDeskTwo);
        studentToken = ticketTestHelper.registerStudentAndLogin(STUDENT_EMAIL);
        staffToken = ticketTestHelper.loginAsNewStaff();
        adminToken = ticketTestHelper.loginAsAdmin();
    }

    @AfterEach
    void closeOpenStreams() {
        streamTestHelper.close();
    }

    @Test
    void createService_withValidRequest_sendsConfigurationChangedToEveryAudience() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(post("/api/admin/services"), "{\"name\":\"Library\",\"code\":\"L\"}")
                .andExpect(status().isCreated());

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void renameService_withValidRequest_sendsConfigurationChangedToEveryAudience() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(put("/api/admin/services/{id}", registration.getId()), "{\"name\":\"Registration Office\"}")
                .andExpect(status().isOk());

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void turnServiceOff_withNoCalledStudent_sendsConfigurationChangedToEveryAudience() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(patch("/api/admin/services/{id}/active", registration.getId()), "{\"active\":false}")
                .andExpect(status().isOk());

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void turnServiceOn_whenServiceIsOff_sendsConfigurationChangedToEveryAudience() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(patch("/api/admin/services/{id}/active", inactiveArchive.getId()), "{\"active\":true}")
                .andExpect(status().isOk());

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void createCounter_withValidRequest_sendsConfigurationChangedToEveryAudience() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(post("/api/admin/counters"), counterBody("Desk 3", registration.getId()))
                .andExpect(status().isCreated());

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void updateCounter_withValidRequest_sendsConfigurationChangedToEveryAudience() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(put("/api/admin/counters/{id}", deskOne.getId()), counterBody("Desk 1 renamed", registration.getId()))
                .andExpect(status().isOk());

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void turnCounterOff_withNoCalledStudent_sendsConfigurationChangedToEveryAudience() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(patch("/api/admin/counters/{id}/active", deskOne.getId()), "{\"active\":false}")
                .andExpect(status().isOk());

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void turnCounterOn_whenCounterIsOff_sendsConfigurationChangedToEveryAudience() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(patch("/api/admin/counters/{id}/active", inactiveDeskTwo.getId()), "{\"active\":true}")
                .andExpect(status().isOk());

        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    @Test
    void createService_withDuplicateName_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(post("/api/admin/services"), "{\"name\":\"Registration\",\"code\":\"Q\"}")
                .andExpect(status().isConflict());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void createService_withDuplicateCode_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(post("/api/admin/services"), "{\"name\":\"Other\",\"code\":\"R\"}")
                .andExpect(status().isConflict());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void createService_withBlankName_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(post("/api/admin/services"), "{\"name\":\"  \",\"code\":\"Q\"}")
                .andExpect(status().isBadRequest());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void renameService_withUnknownId_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(put("/api/admin/services/{id}", UNKNOWN_ID), "{\"name\":\"Anything\"}")
                .andExpect(status().isNotFound());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void renameService_withDuplicateName_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(put("/api/admin/services/{id}", inactiveArchive.getId()), "{\"name\":\"Registration\"}")
                .andExpect(status().isConflict());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void turnServiceOff_whileStudentIsCalled_sendsNothing() throws Exception {
        ticketTestHelper.createCalledTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, deskOne);
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(patch("/api/admin/services/{id}/active", registration.getId()), "{\"active\":false}")
                .andExpect(status().isConflict());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void turnServiceOff_withUnknownId_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(patch("/api/admin/services/{id}/active", UNKNOWN_ID), "{\"active\":false}")
                .andExpect(status().isNotFound());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void createCounter_withDuplicateName_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(post("/api/admin/counters"), counterBody("Desk 1", registration.getId()))
                .andExpect(status().isConflict());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void createCounter_withBlankName_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(post("/api/admin/counters"), counterBody("  ", registration.getId()))
                .andExpect(status().isBadRequest());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void createCounter_withUnknownService_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(post("/api/admin/counters"), counterBody("Desk 3", UNKNOWN_ID))
                .andExpect(status().isNotFound());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void updateCounter_withUnknownId_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(put("/api/admin/counters/{id}", UNKNOWN_ID), counterBody("Desk 3", registration.getId()))
                .andExpect(status().isNotFound());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void turnCounterOff_whileStudentIsCalled_sendsNothing() throws Exception {
        ticketTestHelper.createCalledTicketForRegisteredStudent(STUDENT_EMAIL, registration, 1, deskOne);
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(patch("/api/admin/counters/{id}/active", deskOne.getId()), "{\"active\":false}")
                .andExpect(status().isConflict());

        assertOnlyControlEventWasSent(streams);
    }

    @Test
    void turnCounterOff_withUnknownId_sendsNothing() throws Exception {
        OpenStreams streams = openStreamOfEveryAudience();

        sendAsAdmin(patch("/api/admin/counters/{id}/active", UNKNOWN_ID), "{\"active\":false}")
                .andExpect(status().isNotFound());

        assertOnlyControlEventWasSent(streams);
    }

    private ResultActions sendAsAdmin(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String counterBody(String name, Long serviceId) {
        return "{\"name\":\"%s\",\"serviceId\":%d}".formatted(name, serviceId);
    }

    private void assertOnlyControlEventWasSent(OpenStreams streams) throws Exception {
        queueChangeAnnouncer.announceConfigurationChanged();
        streamTestHelper.assertConfigurationChangedReceivedOnceByEveryAudience(streams);
    }

    private OpenStreams openStreamOfEveryAudience() throws Exception {
        return streamTestHelper.openReadyStreamOfEveryAudience(port, studentToken, staffToken, adminToken);
    }
}
