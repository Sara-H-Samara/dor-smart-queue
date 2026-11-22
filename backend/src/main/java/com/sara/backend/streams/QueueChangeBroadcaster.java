package com.sara.backend.streams;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class QueueChangeBroadcaster {

    private final QueueStreamRegistry queueStreamRegistry;
    private final QueueStreamSender queueStreamSender;
    private final ExecutorService sendExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public QueueChangeBroadcaster(QueueStreamRegistry queueStreamRegistry, QueueStreamSender queueStreamSender) {
        this.queueStreamRegistry = queueStreamRegistry;
        this.queueStreamSender = queueStreamSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void broadcastAfterCommit(QueueChangedEvent event) {
        queueStreamRegistry.findEmittersFor(event.kind())
                .forEach(emitter -> sendExecutor.execute(() -> queueStreamSender.sendChange(emitter, event.kind())));
    }

    @PreDestroy
    public void stopSending() {
        sendExecutor.shutdown();
    }
}
