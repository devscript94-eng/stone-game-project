package com.stonegame.backend.user.service;

import com.stonegame.backend.common.UnauthorizedException;
import com.stonegame.backend.user.dto.UserProfileResponse;
import com.stonegame.backend.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /**
     * Builds the profile response for the currently authenticated user.
     *
     * @param authentication current Spring Security authentication
     * @return current user profile
     * @throws UnauthorizedException if no valid authenticated user is available
     */
    public UserProfileResponse getCurrentUserProfile(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            log.error("User not authenticated");
            throw new UnauthorizedException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user) || user.getId() == null || user.getRole() == null) {
            log.error("User not authenticated");
            throw new UnauthorizedException("User not authenticated");
        }

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
