package com.stonegame.backend.auth.config;

import com.stonegame.backend.auth.service.JwtService;
import com.stonegame.backend.user.model.User;
import com.stonegame.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter used by Spring Security to authenticate incoming HTTP requests.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * Service responsible for JWT parsing and validation.
     */
    private final JwtService jwtService;

    /**
     * Repository used to retrieve users from the database.
     */
    private final UserRepository userRepository;

    /**
     * Creates a new JWT authentication filter.
     *
     * @param jwtService service used to extract and validate JWT tokens
     * @param userRepository repository used to fetch users by email
     */
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Filters each incoming HTTP request to perform JWT authentication.
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param filterChain the filter chain to continue processing the request
     * @throws ServletException if an error occurs during filtering
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Skipping JWT authentication: no Bearer token found for {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String email;

        try {
            email = jwtService.extractSubject(token);
            log.debug("JWT subject extracted successfully for {} {}: email={}", method, path, email);
        } catch (Exception ex) {
            log.warn("JWT subject extraction failed for {} {}: error={}", method, path, ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (email == null) {
            log.warn("JWT subject is null for {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.debug("Security context already populated for {} {}. Skipping JWT authentication.", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null) {
            log.warn("JWT authentication failed: user not found for email={} on {} {}", email, method, path);
            filterChain.doFilter(request, response);
            return;
        }

        boolean tokenValid = jwtService.isTokenValid(token, user.getEmail());

        if (!tokenValid) {
            log.warn("JWT authentication failed: invalid token for userId={}, email={} on {} {}",
                    user.getId(), user.getEmail(), method, path);
            filterChain.doFilter(request, response);
            return;
        }

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        var authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                authorities
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("JWT authentication successful: userId={}, email={}, role={}, method={}, path={}",
                user.getId(), user.getEmail(), user.getRole(), method, path);

        filterChain.doFilter(request, response);
    }
}