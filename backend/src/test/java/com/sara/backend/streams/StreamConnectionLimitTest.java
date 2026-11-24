package com.sara.backend.streams;

import com.sara.backend.configurations.DataSeeder;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.support.StreamTestHelper;
import com.sara.backend.support.TicketTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.sara.backend.support.StreamTestHelper.ADMIN_STREAM_PATH;
import static com.sara.backend.support.StreamTestHelper.PUBLIC_STREAM_PATH;
import static com.sara.backend.support.StreamTestHelper.STAFF_STREAM_PATH;
import static com.sara.backend.support.StreamTestHelper.STUDENT_STREAM_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "dor.streams.max-total-streams=3",
        "dor.streams.max-streams-per-address=2"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Stream connection limits")
class StreamConnectionLimitTest {

    private static final String FIRST_ADDRESS = "10.0.0.1";
    private static final String SECOND_ADDRESS = "10.0.0.2";
    private static final String THIRD_ADDRESS = "10.0.0.3";
    private static final String FOURTH_ADDRESS = "10.0.0.4";
    private static final String FORGED_FORWARDED_ADDRESS = "203.0.113.9";

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

    private StreamTestHelper streamTestHelper;
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
        TicketTestHelper testHelper = new TicketTestHelper(
                mockMvc, userRepository, serviceTypeRepository, ticketRepository, counterRepository, passwordEncoder);
        studentToken = testHelper.registerStudentAndLogin("student@example.com");
        staffToken = testHelper.loginAsNewStaff();
        adminToken = testHelper.loginAsAdmin();
    }

    @AfterEach
    void closeOpenStreams() {
        streamTestHelper.close();
    }

    @Test
    void openStream_whenTotalLimitReached_returnsServiceUnavailable() throws Exception {
        streamTestHelper.openPublicStreamsFromAddress(mockMvc, FIRST_ADDRESS, 2);
        streamTestHelper.openPublicStreamsFromAddress(mockMvc, SECOND_ADDRESS, 1);

        openPublicStreamFrom(THIRD_ADDRESS).andExpect(status().isServiceUnavailable());
    }

    @Test
    void openStream_whenAddressLimitReached_returnsTooManyRequests() throws Exception {
        streamTestHelper.openPublicStreamsFromAddress(mockMvc, FIRST_ADDRESS, 2);

        openPublicStreamFrom(FIRST_ADDRESS).andExpect(status().isTooManyRequests());
    }

    @Test
    void openStream_fromAnotherAddress_whenFirstAddressIsFull_opensStream() throws Exception {
        streamTestHelper.openPublicStreamsFromAddress(mockMvc, FIRST_ADDRESS, 2);

        openPublicStreamFrom(SECOND_ADDRESS)
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }

    @Test
    void openStream_withForgedForwardedForHeader_stillCountsTheDirectAddress() throws Exception {
        streamTestHelper.openPublicStreamsFromAddress(mockMvc, FIRST_ADDRESS, 2);

        streamTestHelper.performStreamRequest(
                        mockMvc,
                        streamTestHelper.streamRequestFromAddress(PUBLIC_STREAM_PATH, FIRST_ADDRESS)
                                .header("X-Forwarded-For", FORGED_FORWARDED_ADDRESS))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void openStream_ofAllFourAudiences_shareTheTotalLimit() throws Exception {
        openPublicStreamFrom(FIRST_ADDRESS).andExpect(request().asyncStarted());
        openStreamFrom(STUDENT_STREAM_PATH, SECOND_ADDRESS, studentToken).andExpect(request().asyncStarted());
        openStreamFrom(STAFF_STREAM_PATH, THIRD_ADDRESS, staffToken).andExpect(request().asyncStarted());

        openStreamFrom(ADMIN_STREAM_PATH, FOURTH_ADDRESS, adminToken)
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void openStream_whenAddressLimitReached_returnsEmptyBody() throws Exception {
        streamTestHelper.openPublicStreamsFromAddress(mockMvc, FIRST_ADDRESS, 2);

        openPublicStreamFrom(FIRST_ADDRESS)
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(""));
    }

    @Test
    void openStream_whenTotalLimitReached_returnsEmptyBody() throws Exception {
        streamTestHelper.openPublicStreamsFromAddress(mockMvc, FIRST_ADDRESS, 2);
        streamTestHelper.openPublicStreamsFromAddress(mockMvc, SECOND_ADDRESS, 1);

        openPublicStreamFrom(THIRD_ADDRESS)
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(""));
    }

    @Test
    void openStream_whenRefused_registersNoEmitter() throws Exception {
        streamTestHelper.openPublicStreamsFromAddress(mockMvc, FIRST_ADDRESS, 2);
        int registeredBeforeRefusal = queueStreamRegistry.findAllEmitters().size();

        openPublicStreamFrom(FIRST_ADDRESS).andExpect(status().isTooManyRequests());

        assertEquals(registeredBeforeRefusal, queueStreamRegistry.findAllEmitters().size());
    }

    private ResultActions openPublicStreamFrom(String clientAddress) throws Exception {
        return streamTestHelper.performStreamRequest(
                mockMvc, streamTestHelper.streamRequestFromAddress(PUBLIC_STREAM_PATH, clientAddress));
    }

    private ResultActions openStreamFrom(String path, String clientAddress, String token) throws Exception {
        return streamTestHelper.performStreamRequest(
                mockMvc,
                streamTestHelper.streamRequestFromAddress(path, clientAddress)
                        .header("Authorization", "Bearer " + token));
    }
}
