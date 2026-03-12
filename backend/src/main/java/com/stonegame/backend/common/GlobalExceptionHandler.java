package com.stonegame.backend.common;

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

    /**
     * Handles domain validation and business conflicts.
     *
     * @param ex thrown exception
     * @return error payload
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex) {
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

        return Map.of(
                "timestamp", Instant.now(),
                "status", 400,
                "error", "Bad Request",
                "message", "Validation failed",
                "fieldErrors", fieldErrors
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleUnauthorized(UnauthorizedException ex) {
        return Map.of(
                "timestamp", Instant.now(),
                "status", 401,
                "error", "Unauthorized",
                "message", ex.getMessage()
        );
    }
}
