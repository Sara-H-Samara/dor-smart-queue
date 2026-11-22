package com.sara.backend.controllers;

import com.sara.backend.services.QueueStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class QueueStreamController {

    private static final String PROXY_BUFFERING_HEADER = "X-Accel-Buffering";

    @Autowired
    private QueueStreamService queueStreamService;

    @GetMapping(value = "/public/queue-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamPublicQueue() {
        return withoutProxyBuffering(queueStreamService.openPublicStream());
    }

    @GetMapping(value = "/tickets/me/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamMyTicket() {
        return withoutProxyBuffering(queueStreamService.openStudentStream());
    }

    @GetMapping(value = "/staff/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamStaffDesk() {
        return withoutProxyBuffering(queueStreamService.openStaffStream());
    }

    @GetMapping(value = "/admin/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamAdminChanges() {
        return withoutProxyBuffering(queueStreamService.openAdminStream());
    }

    private ResponseEntity<SseEmitter> withoutProxyBuffering(SseEmitter emitter) {
        return ResponseEntity.ok()
                .header(PROXY_BUFFERING_HEADER, "no")
                .body(emitter);
    }
}
