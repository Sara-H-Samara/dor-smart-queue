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

public class StreamTestHelper {

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

    public void closeAllStreams() {
        openConnections.forEach(OpenStreamConnection::close);
        openConnections.clear();
        queueStreamRegistry.findAllEmitters().forEach(SseEmitter::complete);
        removeAllEmitters();
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
