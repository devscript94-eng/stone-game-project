package com.stonegame.backend.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * Service responsible for JWT generation and validation.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    /**
     * Creates a JWT service using the configured secret and token expiration.
     *
     * @param secret base64-encoded signing secret
     * @param expirationMillis token lifetime in milliseconds
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMillis
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMillis = expirationMillis;
    }

    /**
     * Generates a signed JWT for the authenticated user.
     *
     * @param subject user email used as token subject
     * @param userId user identifier
     * @param role user role
     * @return signed JWT string
     */
    public String generateToken(String subject, String userId, String role) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the subject from a JWT.
     *
     * @param token JWT token
     * @return token subject
     */
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Checks whether a token is valid for the expected subject and not expired.
     *
     * @param token JWT token
     * @param expectedSubject expected email/subject
     * @return true if valid, otherwise false
     */
    public boolean isTokenValid(String token, String expectedSubject) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject().equals(expectedSubject)
                && claims.getExpiration().after(new Date());
    }

    /**
     * Parses and returns all claims contained in the token.
     *
     * @param token JWT token
     * @return parsed claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
