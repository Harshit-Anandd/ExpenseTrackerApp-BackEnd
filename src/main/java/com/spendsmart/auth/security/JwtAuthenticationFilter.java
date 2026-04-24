/**
 * JwtAuthenticationFilter is a Spring Security Filter that intercepts HTTP requests
 * and validates JWT tokens in the Authorization header.
 *
 * <p>This filter:
 * 1. Extracts the JWT token from the "Authorization: Bearer <token>" header
 * 2. Validates the token's signature and expiration
 * 3. Extracts user identity and role claims
 * 4. Populates the SecurityContext with the authenticated user
 * 5. Passes control to the next filter in the chain
 *
 * <p>For requests without a valid token, the SecurityContext remains empty,
 * allowing downstream security config to handle authorization.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenBlocklistService tokenBlocklistService;

    /**
     * Filter method executed for each incoming HTTP request.
     * Checks for and validates JWT tokens in the Authorization header.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract token from Authorization header (format: "Bearer <token>")
            String token = extractTokenFromRequest(request);
            boolean isRevoked = token != null && tokenBlocklistService.isRevoked(token);

            if (token != null && !isRevoked && jwtUtils.validateToken(token)) {
                // Token is valid, extract claims and populate SecurityContext
                Claims claims = jwtUtils.getClaimsFromToken(token);

                Long userId = claims.get("userId", Long.class);
                String email = claims.getSubject();
                String role = (String) claims.get("role");

                // Create authentication object with role authority
                // Role should be prefixed with "ROLE_" for Spring Security convention
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                // Store additional user info in the principal for downstream services
                authentication.setDetails(new JwtUserDetails(userId, email, role));

                // Set the authentication in the SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT token validated for user: {}", email);
            } else if (isRevoked) {
                log.debug("Rejected revoked JWT token");
            }
        } catch (Exception ex) {
            log.warn("JWT filter processing failed: {}", ex.getMessage());
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from the Authorization header.
     * Expected format: "Bearer <token>"
     *
     * @param request the HTTP request
     * @return the token string, or null if not present or invalid format
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}

