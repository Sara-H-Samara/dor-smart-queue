package com.sara.backend.exception;

import com.sara.backend.dto.response.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    static final String MALFORMED_JSON_MESSAGE = "The request body is not valid JSON.";
    static final String UNEXPECTED_ERROR_MESSAGE = "Something went wrong. Please try again.";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ValidationMessageBuilder validationMessageBuilder;

    public ApiExceptionHandler(ValidationMessageBuilder validationMessageBuilder) {
        this.validationMessageBuilder = validationMessageBuilder;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        if (statusCode.is5xxServerError()) {
            LOGGER.error("Framework error with status {}", statusCode.value(), exception);
            return buildResponse(statusCode, headers, UNEXPECTED_ERROR_MESSAGE);
        }
        return buildResponse(statusCode, headers, describeClientError(exception, body, statusCode));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        String message = validationMessageBuilder.buildInvalidFieldsMessage(
                exception.getBindingResult().getFieldErrors());
        return buildResponse(statusCode, headers, message);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        return buildResponse(statusCode, headers, MALFORMED_JSON_MESSAGE);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void rethrowSecurityException(RuntimeException securityException) {
        throw securityException;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected error", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY, UNEXPECTED_ERROR_MESSAGE);
    }

    private String describeClientError(Exception exception, @Nullable Object body, HttpStatusCode statusCode) {
        if (exception instanceof ResponseStatusException statusException && statusException.getReason() != null) {
            return statusException.getReason();
        }
        if (body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null) {
            return problemDetail.getDetail();
        }
        HttpStatus knownStatus = HttpStatus.resolve(statusCode.value());
        return knownStatus != null ? knownStatus.getReasonPhrase() : "Request failed.";
    }

    private ResponseEntity<Object> buildResponse(HttpStatusCode statusCode, HttpHeaders headers, String message) {
        return ResponseEntity.status(statusCode).headers(headers).body(ApiErrorResponse.of(statusCode, message));
    }
}
