package com.stonegame.backend.auth.config;

import com.stonegame.backend.auth.service.JwtService;
import com.stonegame.backend.user.model.User;
import com.stonegame.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter used by Spring Security to authenticate incoming HTTP requests.
 *
 * <p>This filter runs once per request and is responsible for:
 * <ul>
 *     <li>Extracting the JWT token from the {@code Authorization} header.</li>
 *     <li>Validating the token using {@link JwtService}.</li>
 *     <li>Loading the corresponding {@link User} from the database.</li>
 *     <li>Creating a {@link UsernamePasswordAuthenticationToken} if the token is valid.</li>
 *     <li>Setting the authentication in the {@link SecurityContextHolder} so that
 *     the request is treated as authenticated by Spring Security.</li>
 * </ul>
 *
 * <p>The expected format of the header is:
 * <pre>
 * Authorization: Bearer &lt;jwt-token&gt;
 * </pre>
 *
 * <p>If the token is missing, invalid, or the user cannot be found,
 * the request continues through the filter chain without authentication.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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
     * <p>The filter performs the following steps:
     * <ol>
     *     <li>Reads the {@code Authorization} header.</li>
     *     <li>Extracts the JWT token if it starts with {@code Bearer }.</li>
     *     <li>Extracts the user email (subject) from the token.</li>
     *     <li>Loads the user from the database.</li>
     *     <li>Validates the token.</li>
     *     <li>If valid, creates an authentication object and stores it in the
     *     {@link SecurityContextHolder}.</li>
     * </ol>
     *
     * <p>If any step fails, the filter simply passes the request to the next filter
     * without setting authentication.
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

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String email;

        try {
            email = jwtService.extractSubject(token);
        } catch (Exception ex) {
            filterChain.doFilter(request, response);
            return;
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

            if (user != null && jwtService.isTokenValid(token, user.getEmail())) {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

                var authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        authorities
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}