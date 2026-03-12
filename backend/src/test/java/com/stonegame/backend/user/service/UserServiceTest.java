package com.stonegame.backend.user.service;

import com.stonegame.backend.common.UnauthorizedException;
import com.stonegame.backend.user.dto.UserProfileResponse;
import com.stonegame.backend.user.model.Role;
import com.stonegame.backend.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserService}.
 */
class UserServiceTest {

    private final UserService currentUserService = new UserService();

    @Test
    @DisplayName("getCurrentUserProfile should return authenticated user profile")
    void getCurrentUserProfile_shouldReturnProfile() {
        User user = new User();
        user.setId("user-123");
        user.setUsername("lenin");
        user.setEmail("lenin@company.com");
        user.setRole(Role.USER);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(user, null, null);

        UserProfileResponse response = currentUserService.getCurrentUserProfile(authentication);

        assertNotNull(response);
        assertEquals("user-123", response.id());
        assertEquals("lenin", response.username());
        assertEquals("lenin@company.com", response.email());
        assertEquals("USER", response.role());
    }

    @Test
    @DisplayName("getCurrentUserProfile should throw when authentication is null")
    void getCurrentUserProfile_shouldThrowWhenAuthenticationIsNull() {
        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> currentUserService.getCurrentUserProfile(null)
        );

        assertEquals("User not authenticated", ex.getMessage());
    }

    @Test
    @DisplayName("getCurrentUserProfile should throw when principal is invalid")
    void getCurrentUserProfile_shouldThrowWhenPrincipalIsInvalid() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, null);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> currentUserService.getCurrentUserProfile(authentication)
        );

        assertEquals("User not authenticated", ex.getMessage());
    }

    @Test
    @DisplayName("getCurrentUserProfile should throw when user role is missing")
    void getCurrentUserProfile_shouldThrowWhenRoleIsMissing() {
        User user = new User();
        user.setId("user-123");
        user.setUsername("lenin");
        user.setEmail("lenin@company.com");
        user.setRole(null);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(user, null, null);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> currentUserService.getCurrentUserProfile(authentication)
        );

        assertEquals("User not authenticated", ex.getMessage());
    }
}
