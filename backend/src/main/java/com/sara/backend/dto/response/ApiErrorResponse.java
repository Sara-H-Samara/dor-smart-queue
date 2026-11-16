package com.sara.backend.dto.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ApiErrorResponse(int status, String error, String message, LocalDateTime timestamp) {

    public static ApiErrorResponse of(HttpStatusCode statusCode, String message) {
        HttpStatus knownStatus = HttpStatus.resolve(statusCode.value());
        String reasonPhrase = knownStatus != null ? knownStatus.getReasonPhrase() : "Error";
        return new ApiErrorResponse(
                statusCode.value(), reasonPhrase, message, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }
}
