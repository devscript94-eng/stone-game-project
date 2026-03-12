package com.stonegame.backend.auth.service;

import com.stonegame.backend.auth.dto.AuthResponse;
import com.stonegame.backend.auth.dto.LoginRequest;
import com.stonegame.backend.auth.dto.RegisterRequest;
import com.stonegame.backend.user.model.Role;
import com.stonegame.backend.user.model.User;
import com.stonegame.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("lenin");
        registerRequest.setEmail("lenin@company.com");
        registerRequest.setPassword("Password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("lenin@company.com");
        loginRequest.setPassword("Password123");
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        when(userRepository.existsByEmailIgnoreCase("lenin@company.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("lenin")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-password");

        User savedUser = new User("lenin", "lenin@company.com", "hashed-password", Role.USER);
        savedUser.setId("user-123");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken("lenin@company.com", "user-123", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("user-123", response.getUserId());
        assertEquals("lenin", response.getUsername());
        assertEquals("lenin@company.com", response.getEmail());
        assertEquals("USER", response.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User userToSave = userCaptor.getValue();
        assertEquals("lenin", userToSave.getUsername());
        assertEquals("lenin@company.com", userToSave.getEmail());
        assertEquals("hashed-password", userToSave.getPasswordHash());
        assertEquals(Role.USER, userToSave.getRole());
    }

    @Test
    void register_shouldRejectExistingEmail() {
        when(userRepository.existsByEmailIgnoreCase("lenin@company.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("Email is already in use", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectExistingUsername() {
        when(userRepository.existsByEmailIgnoreCase("lenin@company.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("lenin")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("Username is already in use", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldHandleDuplicateKeyException() {
        when(userRepository.existsByEmailIgnoreCase("lenin@company.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("lenin")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("duplicate"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("Username or email is already in use", ex.getMessage());
    }

    @Test
    void login_shouldReturnTokenWhenCredentialsAreValid() {
        User user = new User("lenin", "lenin@company.com", "hashed-password", Role.USER);
        user.setId("user-123");

        when(userRepository.findByEmailIgnoreCase("lenin@company.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("lenin@company.com", "user-123", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("user-123", response.getUserId());
        assertEquals("lenin", response.getUsername());
        assertEquals("lenin@company.com", response.getEmail());
        assertEquals("USER", response.getRole());
    }

    @Test
    void login_shouldRejectUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("lenin@company.com")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void login_shouldRejectInvalidPassword() {
        User user = new User("lenin", "lenin@company.com", "hashed-password", Role.USER);

        when(userRepository.findByEmailIgnoreCase("lenin@company.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed-password")).thenReturn(false);

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid email or password", ex.getMessage());
    }
}
