package com.sara.backend.streams;

import com.sara.backend.BackendApplication;
import com.sara.backend.support.OpenStreamConnection;
import com.sara.backend.support.StreamTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Stream shutdown")
class StreamShutdownTest {

    private static final Duration LONGEST_ACCEPTED_CLOSE_TIME = Duration.ofSeconds(10);

    @Test
    void closeContext_withOpenStreams_finishesWithoutWaitingForStreams() throws Exception {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class)
                .profiles("test")
                .properties("server.port=0")
                .run();
        int port = context.getEnvironment().getProperty("local.server.port", Integer.class);
        StreamTestHelper streamTestHelper = new StreamTestHelper(context.getBean(QueueStreamRegistry.class));
        OpenStreamConnection firstPublicStream = streamTestHelper.openStream(port, "/api/public/queue-stream");
        OpenStreamConnection secondPublicStream = streamTestHelper.openStream(port, "/api/public/queue-stream");

        long closeStartedAt = System.nanoTime();
        context.close();
        Duration closeTime = Duration.ofNanos(System.nanoTime() - closeStartedAt);

        firstPublicStream.close();
        secondPublicStream.close();
        assertTrue(closeTime.compareTo(LONGEST_ACCEPTED_CLOSE_TIME) < 0,
                "Closing the context took " + closeTime.toSeconds() + " seconds with open streams.");
    }
}
