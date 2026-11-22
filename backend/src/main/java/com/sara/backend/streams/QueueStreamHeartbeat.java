package com.sara.backend.streams;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class QueueStreamHeartbeat {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueStreamHeartbeat.class);
    private static final long HEARTBEAT_INTERVAL_SECONDS = 25;

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
            QueueStreamSendExecutor queueStreamSendExecutor) {
        this.queueStreamRegistry = queueStreamRegistry;
        this.queueStreamSender = queueStreamSender;
        this.queueStreamSendExecutor = queueStreamSendExecutor;
    }

    @PostConstruct
    public void startSendingHeartbeats() {
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeatAndKeepSchedule,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
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
