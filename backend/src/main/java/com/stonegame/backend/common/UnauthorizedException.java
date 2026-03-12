package com.stonegame.backend.common;

/**
 * Exception thrown when a protected resource is accessed without a valid authenticated user.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * Creates an unauthorized exception with the provided message.
     *
     * @param message exception message
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
