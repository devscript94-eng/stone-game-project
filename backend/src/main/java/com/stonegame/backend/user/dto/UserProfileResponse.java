package com.stonegame.backend.user.dto;

/**
 * Response payload for the current authenticated user.
 *
 * @param id user identifier
 * @param username username
 * @param email email
 * @param role role
 */
public record UserProfileResponse(
        String id,
        String username,
        String email,
        String role
) {
}
