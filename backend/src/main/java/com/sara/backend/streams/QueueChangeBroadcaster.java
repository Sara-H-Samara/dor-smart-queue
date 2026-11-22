package com.sara.backend.streams;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class QueueChangeBroadcaster {

    private final QueueStreamRegistry queueStreamRegistry;
    private final QueueStreamSender queueStreamSender;
    private final QueueStreamSendExecutor queueStreamSendExecutor;

    public QueueChangeBroadcaster(
            QueueStreamRegistry queueStreamRegistry,
            QueueStreamSender queueStreamSender,
            QueueStreamSendExecutor queueStreamSendExecutor) {
        this.queueStreamRegistry = queueStreamRegistry;
        this.queueStreamSender = queueStreamSender;
        this.queueStreamSendExecutor = queueStreamSendExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void broadcastAfterCommit(QueueChangedEvent event) {
        queueStreamRegistry.findEmittersFor(event.kind()).forEach(emitter ->
                queueStreamSendExecutor.execute(() -> queueStreamSender.sendChange(emitter, event.kind())));
    }
}
