package com.sara.backend.streams;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class QueueStreamHeartbeat {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueStreamHeartbeat.class);

    private final long heartbeatIntervalMillis;
    private final QueueStreamRegistry queueStreamRegistry;
    private final QueueStreamSender queueStreamSender;
    private final QueueStreamSendExecutor queueStreamSendExecutor;
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread heartbeatThread = new Thread(task, "queue-stream-heartbeat");
        heartbeatThread.setDaemon(true);
        return heartbeatThread;
    });

    public QueueStreamHeartbeat(
            QueueStreamRegistry queueStreamRegistry,
            QueueStreamSender queueStreamSender,
            QueueStreamSendExecutor queueStreamSendExecutor,
            @Value("${dor.streams.heartbeat-interval-millis:25000}") long heartbeatIntervalMillis) {
        this.heartbeatIntervalMillis = heartbeatIntervalMillis;
        this.queueStreamRegistry = queueStreamRegistry;
        this.queueStreamSender = queueStreamSender;
        this.queueStreamSendExecutor = queueStreamSendExecutor;
    }

    @PostConstruct
    public void startSendingHeartbeats() {
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeatAndKeepSchedule,
                heartbeatIntervalMillis, heartbeatIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stopSendingHeartbeats() {
        heartbeatScheduler.shutdownNow();
    }

    public void sendHeartbeatToAllStreams() {
        queueStreamRegistry.findAllEmitters().forEach(emitter ->
                queueStreamSendExecutor.execute(() -> queueStreamSender.sendHeartbeat(emitter)));
    }

    private void sendHeartbeatAndKeepSchedule() {
        try {
            sendHeartbeatToAllStreams();
        } catch (RuntimeException heartbeatFailure) {
            LOGGER.warn("Sending the stream heartbeat failed.", heartbeatFailure);
        }
    }
}
