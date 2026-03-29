package com.stonegame.backend.auth.service;

import com.stonegame.backend.auth.dto.AuthResponse;
import com.stonegame.backend.auth.dto.LoginRequest;
import com.stonegame.backend.auth.dto.RegisterRequest;
import com.stonegame.backend.user.model.Role;
import com.stonegame.backend.user.model.User;
import com.stonegame.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Application service responsible for user registration and login.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";
    private static final String EMAIL_ALREADY_USED_MESSAGE = "Email is already in use";
    private static final String USERNAME_ALREADY_USED_MESSAGE = "Username is already in use";
    private static final String DUPLICATE_USER_MESSAGE = "Username or email is already in use";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Creates the authentication service.
     *
     * @param userRepository user repository
     * @param passwordEncoder encoder used for password hashing
     * @param jwtService JWT generation and validation service
     */
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user and returns an authentication response with JWT.
     *
     * @param request register request payload
     * @return auth response containing token and user info
     */
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String username = normalizeUsername(request.getUsername());

        log.info("Registration requested: username={}, email={}", username, email);

        ensureEmailIsAvailable(email);
        ensureUsernameIsAvailable(username);

        User userToCreate = buildUser(username, email, request.getPassword());

        try {
            User savedUser = userRepository.save(userToCreate);
            log.info("Registration succeeded: userId={}, username={}, email={}",
                    savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());

            return buildAuthResponse(savedUser);
        } catch (DuplicateKeyException ex) {
            log.warn("Registration rejected due to duplicate key: username={}, email={}", username, email);
            throw new IllegalArgumentException(DUPLICATE_USER_MESSAGE);
        }
    }

    /**
     * Authenticates an existing user and returns a JWT.
     *
     * @param request login request payload
     * @return auth response containing token and user info
     */
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        log.info("Login requested: email={}", email);

        User user = findUserByEmail(email);
        validatePassword(request.getPassword(), user, email);

        log.info("Login succeeded: userId={}, username={}, email={}",
                user.getId(), user.getUsername(), user.getEmail());

        return buildAuthResponse(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        return username.trim();
    }

    private void ensureEmailIsAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Registration rejected: email already in use, email={}", email);
            throw new IllegalArgumentException(EMAIL_ALREADY_USED_MESSAGE);
        }
    }

    private void ensureUsernameIsAvailable(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            log.warn("Registration rejected: username already in use, username={}", username);
            throw new IllegalArgumentException(USERNAME_ALREADY_USED_MESSAGE);
        }
    }

    private User buildUser(String username, String email, String rawPassword) {
        String passwordHash = passwordEncoder.encode(rawPassword);
        return new User(username, email, passwordHash, Role.USER);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: email={}", email);
                    return new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
                });
    }

    private void validatePassword(String rawPassword, User user, String email) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            log.warn("Login failed: email={}", email);
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private String generateToken(User user) {
        return jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );
    }
}