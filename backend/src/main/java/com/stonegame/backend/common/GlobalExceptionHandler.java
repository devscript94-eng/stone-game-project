package com.stonegame.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for REST API errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles domain validation and business conflicts.
     *
     * @param ex thrown exception
     * @return error payload
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Business conflict detected: message={}", ex.getMessage());

        return Map.of(
                "timestamp", Instant.now(),
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage()
        );
    }

    /**
     * Handles login failures caused by invalid credentials.
     *
     * @param ex thrown exception
     * @return error payload
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed due to bad credentials: message={}", ex.getMessage());

        return Map.of(
                "timestamp", Instant.now(),
                "status", 401,
                "error", "Unauthorized",
                "message", ex.getMessage()
        );
    }

    /**
     * Handles unauthenticated access to protected user resources.
     *
     * @param ex thrown exception
     * @return error payload
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleRuntime(RuntimeException ex) {
        if (!"User not authenticated".equals(ex.getMessage())) {
            throw ex;
        }

        log.warn("Unauthenticated access attempt detected: message={}", ex.getMessage());

        return Map.of(
                "timestamp", Instant.now(),
                "status", 401,
                "error", "Unauthorized",
                "message", ex.getMessage()
        );
    }

    /**
     * Handles bean validation failures for request DTOs.
     *
     * @param ex thrown exception
     * @return validation error payload
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Request validation failed: fieldErrors={}", fieldErrors);

        return Map.of(
                "timestamp", Instant.now(),
                "status", 400,
                "error", "Bad Request",
                "message", "Validation failed",
                "fieldErrors", fieldErrors
        );
    }

    /**
     * Handles unauthorized access attempts detected within the application.
     *
     * <p>This exception is typically thrown when a user tries to access a resource
     * they are not allowed to access (e.g., accessing another user's match).
     *
     * <p>The error is logged at WARN level as it represents a security-related but expected event.
     *
     * @param ex the thrown {@link UnauthorizedException}
     * @return a standardized error response with HTTP 401 status
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access detected: message={}", ex.getMessage());

        return Map.of(
                "timestamp", Instant.now(),
                "status", 401,
                "error", "Unauthorized",
                "message", ex.getMessage()
        );
    }

    /**
     * Handles all unexpected exceptions not explicitly managed by other handlers.
     *
     * <p>This acts as a global fallback to prevent unhandled exceptions from leaking
     * internal details to the client while ensuring proper logging for debugging.
     *
     * <p>The error is logged at ERROR level with the full stack trace to facilitate
     * root cause analysis.
     *
     * @param ex the unexpected exception
     * @return a generic error response with HTTP 500 status
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleUnexpected(Exception ex) {
        log.error("Unexpected internal server error: message={}", ex.getMessage(), ex);

        return Map.of(
                "timestamp", Instant.now(),
                "status", 500,
                "error", "Internal Server Error",
                "message", "An unexpected error occurred"
        );
    }
}