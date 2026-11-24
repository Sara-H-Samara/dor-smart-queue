package com.sara.backend.streams;

import com.sara.backend.support.OpenStreamConnection;
import com.sara.backend.support.StreamTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static com.sara.backend.support.StreamTestHelper.PUBLIC_STREAM_PATH;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "dor.streams.max-streams-per-address=2",
                "dor.streams.lifetime-millis=1000"})
@ActiveProfiles("test")
@DisplayName("Stream slot release after timeout on a real server")
class StreamSlotReleaseOnTimeoutTest {

    private static final long STREAM_END_WAIT_MILLIS = 8000;

    @LocalServerPort
    private int port;

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
    void openStream_afterStreamTimedOut_succeedsAgain() throws Exception {
        OpenStreamConnection first = streamTestHelper.openReadyStreamWhenSlotIsFree(port, PUBLIC_STREAM_PATH);
        OpenStreamConnection second = streamTestHelper.openReadyStreamWhenSlotIsFree(port, PUBLIC_STREAM_PATH);
        assertTrue(first.awaitEnd(STREAM_END_WAIT_MILLIS));
        assertTrue(second.awaitEnd(STREAM_END_WAIT_MILLIS));

        streamTestHelper.openReadyStreamWhenSlotIsFree(port, PUBLIC_STREAM_PATH);
        streamTestHelper.openReadyStreamWhenSlotIsFree(port, PUBLIC_STREAM_PATH);
    }
}
