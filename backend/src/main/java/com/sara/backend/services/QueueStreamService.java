package com.sara.backend.services;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface QueueStreamService {

    SseEmitter openPublicStream();

    SseEmitter openStudentStream();

    SseEmitter openStaffStream();

    SseEmitter openAdminStream();
}
