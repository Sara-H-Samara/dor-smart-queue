package com.sara.backend.controllers;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.streams.QueueChangeKind;
import com.sara.backend.streams.QueueStreamRegistry;
import com.sara.backend.support.TicketTestHelper;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Queue streams")
class QueueStreamControllerTest {

    private static final String PUBLIC_STREAM_PATH = "/api/public/queue-stream";
    private static final String STUDENT_STREAM_PATH = "/api/tickets/me/stream";
    private static final String STAFF_STREAM_PATH = "/api/staff/stream";
    private static final String ADMIN_STREAM_PATH = "/api/admin/stream";
    private static final String READY_EVENT = "event:ready\ndata:{}\n\n";

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
        TicketTestHelper testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        studentToken = testHelper.registerStudentAndLogin("student@example.com");
        staffToken = testHelper.loginAsNewStaff();
        adminToken = testHelper.loginAsAdmin();
    }

    @AfterEach
    void closeOpenStreams() {
        queueStreamRegistry.findEmittersFor(QueueChangeKind.CONFIGURATION_CHANGED)
                .forEach(queueStreamRegistry::remove);
    }

    @Test
    void streamPublicQueue_withoutToken_opensEventStreamWithReadyEvent() throws Exception {
        MvcResult result = openStreamWithoutToken(PUBLIC_STREAM_PATH)
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(header().string("X-Accel-Buffering", "no"))
                .andReturn();

        assertEquals(READY_EVENT, result.getResponse().getContentAsString());
    }

    @Test
    void streamPublicQueue_withoutToken_registersEmitterForTicketChanges() throws Exception {
        openStreamWithoutToken(PUBLIC_STREAM_PATH).andExpect(request().asyncStarted());

        assertEquals(1, queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED).size());
    }

    @Test
    void streamPublicQueue_withDefaultSettings_opensStreamThatLastsTenMinutes() throws Exception {
        openStreamWithoutToken(PUBLIC_STREAM_PATH).andExpect(request().asyncStarted());

        SseEmitter openEmitter = queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED).get(0);
        assertEquals(600_000L, openEmitter.getTimeout());
    }

    @Test
    void streamMyTicket_withoutToken_returnsUnauthorized() throws Exception {
        openStreamWithoutToken(STUDENT_STREAM_PATH).andExpect(status().isUnauthorized());
    }

    @Test
    void streamMyTicket_withInvalidToken_returnsUnauthorized() throws Exception {
        openStream(STUDENT_STREAM_PATH, "not-a-valid-token").andExpect(status().isUnauthorized());
    }

    @Test
    void streamMyTicket_withoutToken_registersNoEmitter() throws Exception {
        openStreamWithoutToken(STUDENT_STREAM_PATH).andExpect(status().isUnauthorized());

        assertTrue(queueStreamRegistry.findEmittersFor(QueueChangeKind.CONFIGURATION_CHANGED).isEmpty());
    }

    @Test
    void streamMyTicket_asStaff_returnsForbidden() throws Exception {
        openStream(STUDENT_STREAM_PATH, staffToken).andExpect(status().isForbidden());
    }

    @Test
    void streamMyTicket_asAdmin_returnsForbidden() throws Exception {
        openStream(STUDENT_STREAM_PATH, adminToken).andExpect(status().isForbidden());
    }

    @Test
    void streamMyTicket_asStudent_opensEventStreamWithReadyEvent() throws Exception {
        MvcResult result = openStream(STUDENT_STREAM_PATH, studentToken)
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        assertEquals(READY_EVENT, result.getResponse().getContentAsString());
    }

    @Test
    void streamStaffDesk_withoutToken_returnsUnauthorized() throws Exception {
        openStreamWithoutToken(STAFF_STREAM_PATH).andExpect(status().isUnauthorized());
    }

    @Test
    void streamStaffDesk_withInvalidToken_returnsUnauthorized() throws Exception {
        openStream(STAFF_STREAM_PATH, "not-a-valid-token").andExpect(status().isUnauthorized());
    }

    @Test
    void streamStaffDesk_asStudent_returnsForbidden() throws Exception {
        openStream(STAFF_STREAM_PATH, studentToken).andExpect(status().isForbidden());
    }

    @Test
    void streamStaffDesk_asAdmin_returnsForbidden() throws Exception {
        openStream(STAFF_STREAM_PATH, adminToken).andExpect(status().isForbidden());
    }

    @Test
    void streamStaffDesk_asStaff_opensEventStreamWithReadyEvent() throws Exception {
        MvcResult result = openStream(STAFF_STREAM_PATH, staffToken)
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        assertEquals(READY_EVENT, result.getResponse().getContentAsString());
    }

    @Test
    void streamAdminChanges_withoutToken_returnsUnauthorized() throws Exception {
        openStreamWithoutToken(ADMIN_STREAM_PATH).andExpect(status().isUnauthorized());
    }

    @Test
    void streamAdminChanges_withInvalidToken_returnsUnauthorized() throws Exception {
        openStream(ADMIN_STREAM_PATH, "not-a-valid-token").andExpect(status().isUnauthorized());
    }

    @Test
    void streamAdminChanges_asStudent_returnsForbidden() throws Exception {
        openStream(ADMIN_STREAM_PATH, studentToken).andExpect(status().isForbidden());
    }

    @Test
    void streamAdminChanges_asStaff_returnsForbidden() throws Exception {
        openStream(ADMIN_STREAM_PATH, staffToken).andExpect(status().isForbidden());
    }

    @Test
    void streamAdminChanges_asAdmin_opensEventStreamWithReadyEvent() throws Exception {
        MvcResult result = openStream(ADMIN_STREAM_PATH, adminToken)
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        assertEquals(READY_EVENT, result.getResponse().getContentAsString());
    }

    @Test
    void streamAdminChanges_asAdmin_isRegisteredOnlyForConfigurationChanges() throws Exception {
        openStream(ADMIN_STREAM_PATH, adminToken).andExpect(request().asyncStarted());

        assertTrue(queueStreamRegistry.findEmittersFor(QueueChangeKind.TICKET_CHANGED).isEmpty());
        assertEquals(1, queueStreamRegistry.findEmittersFor(QueueChangeKind.CONFIGURATION_CHANGED).size());
    }

    @Test
    void streamMyTicket_afterStreamCompletes_asyncDispatchReturnsOk() throws Exception {
        completeStreamAndDispatchAsync(STUDENT_STREAM_PATH, studentToken).andExpect(status().isOk());
    }

    @Test
    void streamStaffDesk_afterStreamCompletes_asyncDispatchReturnsOk() throws Exception {
        completeStreamAndDispatchAsync(STAFF_STREAM_PATH, staffToken).andExpect(status().isOk());
    }

    @Test
    void streamAdminChanges_afterStreamCompletes_asyncDispatchReturnsOk() throws Exception {
        completeStreamAndDispatchAsync(ADMIN_STREAM_PATH, adminToken).andExpect(status().isOk());
    }

    private ResultActions openStream(String path, String token) throws Exception {
        return mockMvc.perform(get(path)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions openStreamWithoutToken(String path) throws Exception {
        return mockMvc.perform(get(path).accept(MediaType.TEXT_EVENT_STREAM));
    }

    private ResultActions completeStreamAndDispatchAsync(String path, String token) throws Exception {
        MvcResult result = openStream(path, token).andExpect(request().asyncStarted()).andReturn();
        SseEmitter openEmitter = queueStreamRegistry.findEmittersFor(QueueChangeKind.CONFIGURATION_CHANGED).get(0);
        openEmitter.complete();
        return mockMvc.perform(asyncDispatch(result));
    }
}
