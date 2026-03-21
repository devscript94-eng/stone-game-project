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
        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim();

        log.info("Register request received for username={}", username);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Email {} is already in use", email);
            throw new IllegalArgumentException("Email is already in use");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            log.warn("Username {} is already in use", username);
            throw new IllegalArgumentException("Username is already in use");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = new User(username, email, passwordHash, Role.USER);

        try {
            User savedUser = userRepository.save(user);

            String token = jwtService.generateToken(
                    savedUser.getEmail(),
                    savedUser.getId(),
                    savedUser.getRole().name()
            );

            log.info("Registration request succeeded for username={}", username);
            return new AuthResponse(
                    token,
                    savedUser.getId(),
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    savedUser.getRole().name()
            );
        } catch (DuplicateKeyException ex) {
            log.warn("Username {} or email {} is already in use", username, email);
            throw new IllegalArgumentException("Username or email is already in use");
        }
    }

    /**
     * Authenticates an existing user and returns a JWT.
     *
     * @param request login request payload
     * @return auth response containing token and user info
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login request for user={}", request.getEmail());
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("user {} not found", email);
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid email {} or password", email);
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        log.info("Login request succeeded for user={}", user.getUsername());
        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
