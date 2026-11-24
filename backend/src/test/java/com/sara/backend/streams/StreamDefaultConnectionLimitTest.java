package com.sara.backend.streams;

import com.sara.backend.support.StreamTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.sara.backend.support.StreamTestHelper.PUBLIC_STREAM_PATH;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "dor.streams.lifetime-millis=600000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Stream connection default limits")
class StreamDefaultConnectionLimitTest {

    private static final int DEFAULT_STREAMS_PER_ADDRESS = 100;
    private static final int DEFAULT_TOTAL_STREAMS = 1000;
    private static final String SINGLE_ADDRESS = "10.0.0.1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueueStreamRegistry queueStreamRegistry;

    private StreamTestHelper streamTestHelper;

    @BeforeEach
    void createStreamTestHelper() {
        streamTestHelper = new StreamTestHelper(queueStreamRegistry);
    }

    @AfterEach
    void closeOpenStreams() {
        streamTestHelper.close();
    }

    @Test
    void openStream_withDefaultLimits_refusesStreamNumber101FromOneAddress() throws Exception {
        streamTestHelper.openPublicStreamsFromAddress(mockMvc, SINGLE_ADDRESS, DEFAULT_STREAMS_PER_ADDRESS);

        mockMvc.perform(streamTestHelper.streamRequestFromAddress(PUBLIC_STREAM_PATH, SINGLE_ADDRESS))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void openStream_withDefaultLimits_refusesStreamNumber1001WithServiceUnavailable() throws Exception {
        int addressCount = DEFAULT_TOTAL_STREAMS / DEFAULT_STREAMS_PER_ADDRESS;
        for (int addressNumber = 1; addressNumber <= addressCount; addressNumber++) {
            streamTestHelper.openPublicStreamsFromAddress(
                    mockMvc, "10.1.0." + addressNumber, DEFAULT_STREAMS_PER_ADDRESS);
        }

        mockMvc.perform(streamTestHelper.streamRequestFromAddress(PUBLIC_STREAM_PATH, "10.2.0.1"))
                .andExpect(status().isServiceUnavailable());
    }
}
