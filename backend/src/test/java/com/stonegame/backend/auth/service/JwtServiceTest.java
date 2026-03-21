package com.stonegame.backend.auth.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "Q3ZQY3V3b0l4QW5tT3p3Yk1iQm9tZ0x2Y1pZd3JvR2d5Y2x2S2h4dQ==";

    @Test
    @DisplayName("generateToken should create a token whose subject can be extracted")
    void generateToken_shouldCreateTokenWithExtractableSubject() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("john@company.com", "user-123", "USER");

        assertNotNull(token);
        assertEquals("john@company.com", jwtService.extractSubject(token));
    }

    @Test
    @DisplayName("isTokenValid should return true for matching subject and non-expired token")
    void isTokenValid_shouldReturnTrueForValidToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("john@company.com", "user-123", "USER");

        assertTrue(jwtService.isTokenValid(token, "john@company.com"));
    }

    @Test
    @DisplayName("isTokenValid should return false for a different expected subject")
    void isTokenValid_shouldReturnFalseForDifferentSubject() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("john@company.com", "user-123", "USER");

        assertFalse(jwtService.isTokenValid(token, "jane@company.com"));
    }

    @Test
    @DisplayName("extractSubject should throw for malformed token")
    void extractSubject_shouldThrowForMalformedToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        assertThrows(JwtException.class, () -> jwtService.extractSubject("not-a-valid-token"));
    }

    @Test
    @DisplayName("isTokenValid should throw when token is expired")
    void isTokenValid_shouldThrowWhenTokenExpired() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, 1);

        String token = jwtService.generateToken("john@company.com", "user-123", "USER");
        Thread.sleep(10);

        assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(token, "john@company.com"));
    }
}