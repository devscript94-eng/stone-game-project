package com.stonegame.backend.auth;

import com.stonegame.backend.user.Role;
import com.stonegame.backend.user.User;
import com.stonegame.backend.user.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Application service responsible for user registration and login.
 */
@Service
public class AuthService {

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

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
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

            return new AuthResponse(
                    token,
                    savedUser.getId(),
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    savedUser.getRole().name()
            );
        } catch (DuplicateKeyException ex) {
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
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
