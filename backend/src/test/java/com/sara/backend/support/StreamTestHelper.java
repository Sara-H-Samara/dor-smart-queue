package com.sara.backend.support;

import com.sara.backend.streams.QueueStreamRegistry;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StreamTestHelper {

    public static final String PUBLIC_STREAM_PATH = "/api/public/queue-stream";
    public static final String STUDENT_STREAM_PATH = "/api/tickets/me/stream";
    public static final String STAFF_STREAM_PATH = "/api/staff/stream";
    public static final String ADMIN_STREAM_PATH = "/api/admin/stream";
    public static final String READY_EVENT = "event:ready\ndata:{}";
    public static final String HEARTBEAT_EVENT = ":heartbeat";
    public static final String TICKET_CHANGED_EVENT = "event:ticket-changed\ndata:{}";
    public static final String CONFIGURATION_CHANGED_EVENT = "event:configuration-changed\ndata:{}";
    public static final long EVENT_WAIT_MILLIS = 5000;
    public static final long NO_EVENT_WAIT_MILLIS = 300;

    private final QueueStreamRegistry queueStreamRegistry;
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final List<OpenStreamConnection> openConnections = new CopyOnWriteArrayList<>();

    public StreamTestHelper(QueueStreamRegistry queueStreamRegistry) {
        this.queueStreamRegistry = queueStreamRegistry;
    }

    public OpenStreamConnection openStream(int port, String path) throws IOException, InterruptedException {
        return openStream(streamRequest(port, path).build());
    }

    public OpenStreamConnection openStream(int port, String path, String token)
            throws IOException, InterruptedException {
        return openStream(streamRequest(port, path).header("Authorization", "Bearer " + token).build());
    }

    public OpenStreamConnection openReadyStream(int port, String path) throws IOException, InterruptedException {
        return confirmReady(openStream(port, path));
    }

    public OpenStreamConnection openReadyStream(int port, String path, String token)
            throws IOException, InterruptedException {
        return confirmReady(openStream(port, path, token));
    }

    public OpenStreams openReadyStreamOfEveryAudience(
            int port, String studentToken, String staffToken, String adminToken)
            throws IOException, InterruptedException {
        return new OpenStreams(
                openReadyStream(port, PUBLIC_STREAM_PATH),
                openReadyStream(port, STUDENT_STREAM_PATH, studentToken),
                openReadyStream(port, STAFF_STREAM_PATH, staffToken),
                openReadyStream(port, ADMIN_STREAM_PATH, adminToken));
    }

    public void assertTicketChangedReceivedByPublicStudentAndStaff(OpenStreams streams) {
        assertEquals(TICKET_CHANGED_EVENT, streams.publicStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertEquals(TICKET_CHANGED_EVENT, streams.studentStream().awaitEvent(EVENT_WAIT_MILLIS));
        assertEquals(TICKET_CHANGED_EVENT, streams.staffStream().awaitEvent(EVENT_WAIT_MILLIS));
    }

    public void assertHeartbeatsReceived(OpenStreamConnection stream, int heartbeatCount) {
        for (int received = 0; received < heartbeatCount; received++) {
            assertEquals(HEARTBEAT_EVENT, stream.awaitEvent(EVENT_WAIT_MILLIS));
        }
    }

    public String awaitEventSkippingHeartbeats(OpenStreamConnection stream) {
        long deadline = System.currentTimeMillis() + EVENT_WAIT_MILLIS;
        String event = stream.awaitEvent(EVENT_WAIT_MILLIS);
        while (HEARTBEAT_EVENT.equals(event) && System.currentTimeMillis() < deadline) {
            event = stream.awaitEvent(EVENT_WAIT_MILLIS);
        }
        return event;
    }

    public void assertNoEventReceived(OpenStreamConnection stream) {
        assertNull(stream.awaitEvent(NO_EVENT_WAIT_MILLIS));
    }

    public void assertConfigurationChangedReceivedOnceByEveryAudience(OpenStreams streams)
            throws InterruptedException {
        for (OpenStreamConnection stream : streams.all()) {
            assertEquals(CONFIGURATION_CHANGED_EVENT, stream.awaitEvent(EVENT_WAIT_MILLIS));
        }
        Thread.sleep(NO_EVENT_WAIT_MILLIS);
        for (OpenStreamConnection stream : streams.all()) {
            assertNull(stream.awaitEvent(0));
        }
    }

    public int fetchStatusCode(int port, String path, Duration timeout) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri(port, path)).timeout(timeout).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    public boolean awaitNoRegisteredEmitters(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (queueStreamRegistry.findAllEmitters().isEmpty()) {
                return true;
            }
            Thread.sleep(50);
        }
        return queueStreamRegistry.findAllEmitters().isEmpty();
    }

    public void removeAllEmitters() {
        queueStreamRegistry.findAllEmitters().forEach(queueStreamRegistry::remove);
    }

    public void close() {
        closeAllStreams();
        httpClient.close();
    }

    private void closeAllStreams() {
        openConnections.forEach(OpenStreamConnection::close);
        openConnections.clear();
        queueStreamRegistry.findAllEmitters().forEach(SseEmitter::complete);
        removeAllEmitters();
    }

    private OpenStreamConnection confirmReady(OpenStreamConnection connection) {
        assertEquals(200, connection.statusCode());
        assertEquals(READY_EVENT, connection.awaitEvent(EVENT_WAIT_MILLIS));
        return connection;
    }

    private OpenStreamConnection openStream(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<Stream<String>> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
        OpenStreamConnection connection = new OpenStreamConnection(response);
        openConnections.add(connection);
        return connection;
    }

    private HttpRequest.Builder streamRequest(int port, String path) {
        return HttpRequest.newBuilder(uri(port, path)).header("Accept", "text/event-stream").GET();
    }

    private URI uri(int port, String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
