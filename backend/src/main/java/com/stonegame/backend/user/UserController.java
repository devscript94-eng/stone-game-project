package com.stonegame.backend.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints related to users.
 *
 * <p>The "/api" prefix is applied globally through application properties.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param authentication current Spring Security authentication
     * @return current user profile
     */
    @GetMapping("/me")
    public UserProfileResponse me(Authentication authentication) {
        return userService.getCurrentUserProfile(authentication);
    }
}
