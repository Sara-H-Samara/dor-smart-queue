package com.sara.backend.support;

import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class OpenStreamConnection implements AutoCloseable {

    private final int statusCode;
    private final Stream<String> responseLines;
    private final BlockingQueue<String> receivedEvents = new LinkedBlockingQueue<>();
    private final CountDownLatch streamEnded = new CountDownLatch(1);
    private volatile boolean endedCleanly;

    OpenStreamConnection(HttpResponse<Stream<String>> response) {
        this.statusCode = response.statusCode();
        this.responseLines = response.body();
        Thread readerThread = new Thread(this::readEventsUntilStreamEnds, "test-stream-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public int statusCode() {
        return statusCode;
    }

    public String awaitEvent(long timeoutMillis) {
        try {
            return receivedEvents.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a stream event.", interrupted);
        }
    }

    public boolean awaitEnd(long timeoutMillis) {
        try {
            return streamEnded.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the stream to end.", interrupted);
        }
    }

    public boolean endedCleanly() {
        return endedCleanly;
    }

    @Override
    public void close() {
        responseLines.close();
    }

    private void readEventsUntilStreamEnds() {
        StringBuilder currentEvent = new StringBuilder();
        try {
            responseLines.forEach(line -> {
                if (!line.isEmpty()) {
                    if (currentEvent.length() > 0) {
                        currentEvent.append('\n');
                    }
                    currentEvent.append(line);
                } else if (currentEvent.length() > 0) {
                    receivedEvents.add(currentEvent.toString());
                    currentEvent.setLength(0);
                }
            });
            endedCleanly = true;
        } catch (UncheckedIOException brokenStream) {
            endedCleanly = false;
        } finally {
            streamEnded.countDown();
        }
    }
}
