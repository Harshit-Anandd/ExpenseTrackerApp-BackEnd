/**
 * JwtUtils provides utility methods for JWT (JSON Web Token) generation, validation,
 * and claim extraction.
 *
 * <p>This class uses the JJWT library to securely sign and verify JWTs. It handles:
 * - Access token generation (24 hour expiration)
 * - Refresh token generation (7 day expiration)
 * - Token validation (signature, expiration, format)
 * - Claim extraction for identity and role-based access control
 *
 * <p>The JWT secret is loaded from application properties (environment variable).
 * All timestamps are in UTC for consistency.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    /**
     * Generate a JWT access token for the authenticated user.
     * Token expires in 24 hours by default.
     *
     * @param userId the user's unique identifier
     * @param email the user's email address
     * @param role the user's role (USER or ADMIN)
     * @return a signed JWT string
     */
    public String generateAccessToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);

        return createToken(claims, email, accessTokenExpiration, false);
    }

    /**
     * Generate a JWT refresh token for token renewal.
     * Token expires in 7 days by default.
     *
     * @param userId the user's unique identifier
     * @param email the user's email address
     * @return a signed JWT string
     */
    public String generateRefreshToken(Long userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("type", "REFRESH");

        return createToken(claims, email, refreshTokenExpiration, true);
    }

    /**
     * Create a JWT token with specified claims and expiration.
     *
     * @param claims additional claims to include in the token
     * @param subject the subject (typically the email or user ID)
     * @param expirationTime expiration time in milliseconds
     * @param isRefreshToken true if this is a refresh token
     * @return a signed JWT string
     */
    private String createToken(Map<String, Object> claims, String subject,
                               long expirationTime, boolean isRefreshToken) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validate a JWT token's signature and expiration.
     *
     * @param token the JWT string to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extract claims from a JWT token.
     * Only call this after validating the token.
     *
     * @param token the JWT string
     * @return Claims object containing all claims in the token
     */
    public Claims getClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract the userId claim from a JWT token.
     *
     * @param token the JWT string
     * @return the userId as Long
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extract the email (subject) from a JWT token.
     *
     * @param token the JWT string
     * @return the email address
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Extract the role claim from a JWT token.
     *
     * @param token the JWT string
     * @return the role (USER or ADMIN)
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (String) claims.get("role");
    }

    /**
     * Check if a token is a refresh token (has type claim = "REFRESH").
     *
     * @param token the JWT string
     * @return true if this is a refresh token, false otherwise
     */
    public boolean isRefreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return "REFRESH".equals(claims.get("type"));
    }

    /**
     * Get the time remaining (in milliseconds) before the token expires.
     *
     * @param token the JWT string
     * @return milliseconds until expiration, or negative if already expired
     */
    public long getTimeRemainingInToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
}

