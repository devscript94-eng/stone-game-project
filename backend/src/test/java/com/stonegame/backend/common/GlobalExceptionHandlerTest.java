package com.stonegame.backend.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleIllegalArgument should return 409 conflict payload")
    void handleIllegalArgument_shouldReturnConflictPayload() {
        Map<String, Object> response = handler.handleIllegalArgument(new IllegalArgumentException("Duplicate email"));

        assertEquals(409, response.get("status"));
        assertEquals("Conflict", response.get("error"));
        assertEquals("Duplicate email", response.get("message"));
        assertNotNull(response.get("timestamp"));
    }

    @Test
    @DisplayName("handleBadCredentials should return 401 unauthorized payload")
    void handleBadCredentials_shouldReturnUnauthorizedPayload() {
        Map<String, Object> response = handler.handleBadCredentials(new BadCredentialsException("Invalid credentials"));

        assertEquals(401, response.get("status"));
        assertEquals("Unauthorized", response.get("error"));
        assertEquals("Invalid credentials", response.get("message"));
    }

    @Test
    @DisplayName("handleRuntime should return 401 for unauthenticated user")
    void handleRuntime_shouldReturnUnauthorizedForUnauthenticatedUser() {
        Map<String, Object> response = handler.handleRuntime(new RuntimeException("User not authenticated"));

        assertEquals(401, response.get("status"));
        assertEquals("Unauthorized", response.get("error"));
        assertEquals("User not authenticated", response.get("message"));
    }

    @Test
    @DisplayName("handleRuntime should rethrow unrelated runtime exceptions")
    void handleRuntime_shouldRethrowUnrelatedRuntimeExceptions() {
        RuntimeException ex = new RuntimeException("Some other runtime error");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handler.handleRuntime(ex));

        assertSame(ex, thrown);
    }

    @Test
    @DisplayName("handleValidation should return 400 with field errors")
    void handleValidation_shouldReturnBadRequestWithFieldErrors() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must be a well-formed email address"));
        bindingResult.addError(new FieldError("request", "password", "size must be between 8 and 100"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        Map<String, Object> response = handler.handleValidation(ex);

        assertEquals(400, response.get("status"));
        assertEquals("Bad Request", response.get("error"));
        assertEquals("Validation failed", response.get("message"));

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.get("fieldErrors");

        assertEquals("must be a well-formed email address", fieldErrors.get("email"));
        assertEquals("size must be between 8 and 100", fieldErrors.get("password"));
    }

    @Test
    @DisplayName("handleUnauthorized should return 401 payload")
    void handleUnauthorized_shouldReturnUnauthorizedPayload() {
        Map<String, Object> response = handler.handleUnauthorized(
                new UnauthorizedException("User not allowed to access this match")
        );

        assertEquals(401, response.get("status"));
        assertEquals("Unauthorized", response.get("error"));
        assertEquals("User not allowed to access this match", response.get("message"));
    }
}