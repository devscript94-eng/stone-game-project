package com.stonegame.backend.auth.config;

import com.stonegame.backend.auth.service.JwtService;
import com.stonegame.backend.user.model.Role;
import com.stonegame.backend.user.model.User;
import com.stonegame.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
    private final FilterChain filterChain = mock(FilterChain.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should skip authentication when Authorization header is missing")
    void shouldSkipAuthenticationWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should skip authentication when token subject extraction fails")
    void shouldSkipAuthenticationWhenTokenInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractSubject("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).extractSubject("invalid-token");
        verifyNoInteractions(userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should skip authentication when user is not found")
    void shouldSkipAuthenticationWhenUserNotFound() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractSubject("valid-token")).thenReturn("max@company.com");
        when(userRepository.findByEmailIgnoreCase("max@company.com")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).extractSubject("valid-token");
        verify(userRepository).findByEmailIgnoreCase("max@company.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should set authentication when token is valid")
    void shouldSetAuthenticationWhenTokenValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User();
        user.setId("user-123");
        user.setEmail("max@company.com");
        user.setUsername("max");
        user.setRole(Role.USER);

        when(jwtService.extractSubject("valid-token")).thenReturn("max@company.com");
        when(userRepository.findByEmailIgnoreCase("max@company.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid-token", "max@company.com")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should not overwrite existing authentication")
    void shouldNotOverwriteExistingAuthentication() throws Exception {
        var existingAuth = mock(org.springframework.security.core.Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractSubject("valid-token")).thenReturn("max@company.com");

        filter.doFilterInternal(request, response, filterChain);

        assertSame(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).extractSubject("valid-token");
        verifyNoInteractions(userRepository);
        verify(filterChain).doFilter(request, response);
    }
}
