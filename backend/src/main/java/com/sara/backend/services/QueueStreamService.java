package com.sara.backend.services;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface QueueStreamService {

    SseEmitter openPublicStream(String clientAddress);

    SseEmitter openStudentStream(String clientAddress);

    SseEmitter openStaffStream(String clientAddress);

    SseEmitter openAdminStream(String clientAddress);
}
