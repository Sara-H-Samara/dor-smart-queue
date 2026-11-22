package com.sara.backend.streams;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class QueueStreamSendExecutor {

    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public void execute(Runnable sendTask) {
        virtualThreadExecutor.execute(sendTask);
    }

    @PreDestroy
    public void stopSending() {
        virtualThreadExecutor.shutdown();
    }
}
