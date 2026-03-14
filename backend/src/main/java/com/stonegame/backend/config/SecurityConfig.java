package com.stonegame.backend.config;

import com.stonegame.backend.auth.config.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Spring Security configuration for the application.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures the security filter chain with stateless JWT authentication.
     *
     * @param http http security builder
     * @param jwtAuthenticationFilter JWT filter applied before username/password auth filter
     * @return configured security filter chain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        PathPatternRequestMatcher.Builder api =
                PathPatternRequestMatcher.withDefaults().basePath("/api");

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(api.matcher("/auth/**")).permitAll()
                        .requestMatchers(api.matcher("/actuator/health")).permitAll()
                        .requestMatchers(api.matcher("/actuator/info")).permitAll()
                        .requestMatchers(api.matcher("/actuator/prometheus")).permitAll()
                        .requestMatchers(api.matcher("/actuator/metrics/**")).permitAll()
                        .requestMatchers(api.matcher("/actuator/loggers/**")).permitAll()
                        .requestMatchers(api.matcher("/ws")).permitAll()
                        .requestMatchers(api.matcher("/ws/**")).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}