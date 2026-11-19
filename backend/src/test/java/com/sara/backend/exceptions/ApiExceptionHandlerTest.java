package com.sara.backend.exceptions;

import com.sara.backend.dtos.responses.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler(new ValidationMessageBuilder());

    @Test
    void handleUnexpected_anyException_returnsInternalServerErrorWithoutDetails() {
        ResponseEntity<Object> response = handler.handleUnexpected(new IllegalStateException("database password leaked"));

        ApiErrorResponse body = (ApiErrorResponse) response.getBody();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, body.status());
        assertEquals("Internal Server Error", body.error());
        assertEquals("Something went wrong. Please try again.", body.message());
        assertFalse(body.message().contains("password"));
    }

    @Test
    void rethrowSecurityException_accessDenied_throwsSameException() {
        AccessDeniedException accessDenied = new AccessDeniedException("denied");

        AccessDeniedException thrown =
                assertThrows(AccessDeniedException.class, () -> handler.rethrowSecurityException(accessDenied));

        assertSame(accessDenied, thrown);
    }

    @Test
    void rethrowSecurityException_authenticationFailure_throwsSameException() {
        BadCredentialsException authenticationFailure = new BadCredentialsException("bad");

        BadCredentialsException thrown = assertThrows(
                BadCredentialsException.class, () -> handler.rethrowSecurityException(authenticationFailure));

        assertSame(authenticationFailure, thrown);
    }
}
