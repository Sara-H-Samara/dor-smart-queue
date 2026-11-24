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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static com.sara.backend.support.StreamTestHelper.CONFIGURATION_CHANGED_EVENT;
import static com.sara.backend.support.StreamTestHelper.PUBLIC_STREAM_PATH;
import static com.sara.backend.support.StreamTestHelper.SLOT_RELEASE_WAIT_MILLIS;
import static com.sara.backend.support.StreamTestHelper.TOO_MANY_REQUESTS_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "dor.streams.max-streams-per-address=2",
                "dor.streams.heartbeat-interval-millis=200"})
@ActiveProfiles("test")
@DisplayName("Stream slot release on a real server")
class StreamSlotReleaseTest {

    private static final int OPEN_AND_CLOSE_CYCLE_COUNT = 20;

    @LocalServerPort
    private int port;

    @Autowired
    private QueueStreamRegistry queueStreamRegistry;

    @Autowired
    private QueueStreamSender queueStreamSender;

    @Autowired
    private QueueStreamShutdown queueStreamShutdown;

    @Autowired
    private QueueChangeAnnouncer queueChangeAnnouncer;

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
    void openStream_afterClientClosedStream_succeedsAgain() throws Exception {
        List<OpenStreamConnection> fullAddress = openStreamsUntilAddressIsFull();

        fullAddress.getFirst().close();

        streamTestHelper.openReadyStreamWhenSlotIsFree(port, PUBLIC_STREAM_PATH);
    }

    @Test
    void openStream_afterFailedSend_succeedsAgain() throws Exception {
        List<OpenStreamConnection> fullAddress = openStreamsUntilAddressIsFull();
        fullAddress.forEach(OpenStreamConnection::close);

        sendHeartbeatsUntilEveryClosedStreamIsRemoved();

        streamTestHelper.openReadyStreamWhenSlotIsFree(port, PUBLIC_STREAM_PATH);
        streamTestHelper.openReadyStreamWhenSlotIsFree(port, PUBLIC_STREAM_PATH);
    }

    @Test
    void openStream_afterServerShutdownCompletesStreams_releasesEverySlot() throws Exception {
        openStreamsUntilAddressIsFull();

        queueStreamShutdown.completeAllStreams();

        openStreamsUntilAddressIsFull();
    }

    @Test
    void openStream_afterManyOpenAndCloseCycles_leavesNoReservedSlot() throws Exception {
        for (int cycle = 0; cycle < OPEN_AND_CLOSE_CYCLE_COUNT; cycle++) {
            openStreamsUntilAddressIsFull();
            queueStreamRegistry.findAllEmitters().forEach(SseEmitter::complete);
        }

        openStreamsUntilAddressIsFull();
    }

    @Test
    void openStream_whenRefused_keepsExistingStreamsReceivingEvents() throws Exception {
        List<OpenStreamConnection> fullAddress = openStreamsUntilAddressIsFull();

        queueChangeAnnouncer.announceConfigurationChanged();

        for (OpenStreamConnection existingStream : fullAddress) {
            assertEquals(CONFIGURATION_CHANGED_EVENT, streamTestHelper.awaitEventSkippingHeartbeats(existingStream));
        }
    }

    private List<OpenStreamConnection> openStreamsUntilAddressIsFull() throws Exception {
        OpenStreamConnection first = streamTestHelper.openReadyStreamWhenSlotIsFree(port, PUBLIC_STREAM_PATH);
        OpenStreamConnection second = streamTestHelper.openReadyStreamWhenSlotIsFree(port, PUBLIC_STREAM_PATH);
        assertEquals(TOO_MANY_REQUESTS_STATUS, streamTestHelper.openStream(port, PUBLIC_STREAM_PATH).statusCode());
        return List.of(first, second);
    }

    private void sendHeartbeatsUntilEveryClosedStreamIsRemoved() throws InterruptedException {
        long deadline = System.currentTimeMillis() + SLOT_RELEASE_WAIT_MILLIS;
        while (!queueStreamRegistry.findAllEmitters().isEmpty() && System.currentTimeMillis() < deadline) {
            queueStreamRegistry.findAllEmitters().forEach(queueStreamSender::sendHeartbeat);
            Thread.sleep(50);
        }
        assertTrue(queueStreamRegistry.findAllEmitters().isEmpty());
    }
}
