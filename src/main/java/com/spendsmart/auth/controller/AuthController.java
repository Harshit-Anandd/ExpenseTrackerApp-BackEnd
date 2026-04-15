/**
 * AuthController exposes REST API endpoints for authentication and user management.
 *
 * <p>Provides endpoints for:
 * - User registration (POST /auth/register)
 * - User login (POST /auth/login)
 * - Token refresh (POST /auth/refresh)
 * - Logout (POST /auth/logout)
 * - Profile retrieval (GET /auth/profile)
 * - Profile update (PUT /auth/profile)
 * - Password change (PUT /auth/password)
 * - Currency update (PUT /auth/currency)
 * - Account deactivation (PUT /auth/deactivate)
 *
 * <p>All endpoints return JSON responses. Authentication is required for all
 * endpoints except /auth/register and /auth/login.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.controller;

import com.spendsmart.auth.dto.*;
import com.spendsmart.auth.security.JwtUserDetails;
import com.spendsmart.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://spendsmart.app"})
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Register a new user with email and password.
     *
     * HTTP: POST /auth/register
     * Access: Public
     *
     * @param registerDto the registration request
     * @return 201 Created with AuthResponseDto containing tokens
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterDto registerDto) {
        log.info("Register endpoint called for email: {}", registerDto.getEmail());
        AuthResponseDto response = authService.register(registerDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate a user with email and password.
     *
     * HTTP: POST /auth/login
     * Access: Public
     *
     * @param loginDto the login request
     * @return 200 OK with AuthResponseDto containing tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
        log.info("Login endpoint called for email: {}", loginDto.getEmail());
        AuthResponseDto response = authService.login(loginDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh an expired access token.
     *
     * HTTP: POST /auth/refresh
     * Access: Public
     *
     * @param refreshTokenDto contains the refresh token
     * @return 200 OK with AuthResponseDto with new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refreshToken(@Valid @RequestBody RefreshTokenDto refreshTokenDto) {
        log.info("Token refresh endpoint called");
        AuthResponseDto response = authService.refreshToken(refreshTokenDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout a user (invalidate current session).
     *
     * HTTP: POST /auth/logout
     * Access: Protected (requires valid JWT)
     *
     * @param authentication the authenticated user (Spring Security)
     * @return 200 OK with success message
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        log.info("Logout endpoint called for user: {}", authentication.getName());
        authService.logout(extractTokenFromContext(authentication));
        return ResponseEntity.ok(new MessageDto("Logged out successfully"));
    }

    /**
     * Get the authenticated user's profile.
     *
     * HTTP: GET /auth/profile
     * Access: Protected (requires valid JWT)
     *
     * @param authentication the authenticated user (Spring Security)
     * @return 200 OK with UserProfileDto
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(Authentication authentication) {
        Long userId = extractUserIdFromContext(authentication);
        log.info("Get profile endpoint called for user ID: {}", userId);
        UserProfileDto profile = authService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update the authenticated user's profile.
     *
     * HTTP: PUT /auth/profile
     * Access: Protected (requires valid JWT)
     *
     * @param profileUpdateDto the profile update request
     * @param authentication the authenticated user (Spring Security)
     * @return 200 OK with updated UserProfileDto
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @Valid @RequestBody ProfileUpdateDto profileUpdateDto,
            Authentication authentication) {
        Long userId = extractUserIdFromContext(authentication);
        log.info("Update profile endpoint called for user ID: {}", userId);
        UserProfileDto updatedProfile = authService.updateProfile(userId, profileUpdateDto);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Change the authenticated user's password.
     *
     * HTTP: PUT /auth/password
     * Access: Protected (requires valid JWT)
     *
     * @param passwordChangeDto the password change request
     * @param authentication the authenticated user (Spring Security)
     * @return 200 OK with success message
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody PasswordChangeDto passwordChangeDto,
            Authentication authentication) {
        Long userId = extractUserIdFromContext(authentication);
        log.info("Change password endpoint called for user ID: {}", userId);
        authService.changePassword(userId, passwordChangeDto);
        return ResponseEntity.ok(new MessageDto("Password changed successfully"));
    }

    /**
     * Update the authenticated user's currency preference.
     *
     * HTTP: PUT /auth/currency
     * Access: Protected (requires valid JWT)
     *
     * @param request contains the new currency code
     * @param authentication the authenticated user (Spring Security)
     * @return 200 OK with success message
     */
    @PutMapping("/currency")
    public ResponseEntity<?> updateCurrency(
            @RequestBody CurrencyUpdateDto request,
            Authentication authentication) {
        Long userId = extractUserIdFromContext(authentication);
        log.info("Update currency endpoint called for user ID: {} to currency: {}", userId, request.getCurrency());
        authService.updateCurrency(userId, request.getCurrency());
        return ResponseEntity.ok(new MessageDto("Currency updated successfully"));
    }

    /**
     * Deactivate the authenticated user's account.
     *
     * HTTP: PUT /auth/deactivate
     * Access: Protected (requires valid JWT)
     *
     * @param authentication the authenticated user (Spring Security)
     * @return 200 OK with success message
     */
    @PutMapping("/deactivate")
    public ResponseEntity<?> deactivateAccount(Authentication authentication) {
        Long userId = extractUserIdFromContext(authentication);
        log.info("Deactivate account endpoint called for user ID: {}", userId);
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(new MessageDto("Account deactivated successfully"));
    }

    /**
     * Extract userId from the authenticated user's JWT claims.
     *
     * @param authentication the Spring Security authentication object
     * @return the userId
     */
    private Long extractUserIdFromContext(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtUserDetails) {
            return ((JwtUserDetails) authentication.getDetails()).getUserId();
        }
        throw new IllegalArgumentException("Invalid authentication context");
    }

    /**
     * Extract JWT token from authentication context.
     *
     * @param authentication the Spring Security authentication object
     * @return the JWT token (not used in this implementation, but kept for future use)
     */
    private String extractTokenFromContext(Authentication authentication) {
        // In a stateless JWT setup, we don't store tokens server-side
        // This is a placeholder for future blocklist implementation
        return "";
    }
}

