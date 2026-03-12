package com.stonegame.backend.user.controller;

import com.stonegame.backend.auth.config.JwtAuthenticationFilter;
import com.stonegame.backend.common.GlobalExceptionHandler;
import com.stonegame.backend.user.dto.UserProfileResponse;
import com.stonegame.backend.user.model.Role;
import com.stonegame.backend.user.model.User;
import com.stonegame.backend.user.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer tests for {@link UserController}.
 */
@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private UserService currentUserService;

    @Test
    @DisplayName("GET /users/me should return authenticated user profile")
    void me_shouldReturnAuthenticatedUser() throws Exception {
        User user = new User();
        user.setId("user-123");
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setRole(Role.USER);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, null);

        UserProfileResponse response = new UserProfileResponse(
                "user-123",
                "john",
                "john@example.com",
                "USER"
        );

        when(currentUserService.getCurrentUserProfile(any())).thenReturn(response);

        mockMvc.perform(get("/users/me")
                        .with(authentication(auth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("GET /users/me should return 401 when user is not authenticated")
    void me_shouldReturnUnauthorizedWhenUserMissing() throws Exception {
        when(currentUserService.getCurrentUserProfile(any()))
                .thenThrow(new com.stonegame.backend.common.UnauthorizedException("User not authenticated"));

        mockMvc.perform(get("/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User not authenticated"));
    }
}