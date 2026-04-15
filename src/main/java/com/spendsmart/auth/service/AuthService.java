/**
 * AuthService interface defines all authentication and user management operations
 * for the SpendSmart platform.
 *
 * <p>This interface provides a contract for authentication business logic,
 * including registration, login, token management, profile updates,
 * and account management.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.service;

import com.spendsmart.auth.dto.*;
import com.spendsmart.auth.entity.User;

public interface AuthService {

    /**
     * Register a new user with email and password.
     *
     * @param registerDto the registration request containing email, password, and full name
     * @return AuthResponseDto with access and refresh tokens
     * @throws UserAlreadyExistsException if email is already registered
     */
    AuthResponseDto register(RegisterDto registerDto);

    /**
     * Authenticate a user with email and password.
     *
     * @param loginDto the login request containing email and password
     * @return AuthResponseDto with access and refresh tokens
     * @throws InvalidCredentialsException if credentials are invalid
     * @throws DeactivatedAccountException if account is inactive
     */
    AuthResponseDto login(LoginDto loginDto);

    /**
     * Handle OAuth2 login/registration for Google users.
     *
     * @param email the user's email from Google profile
     * @param name the user's full name from Google profile
     * @param avatarUrl the user's profile picture URL from Google
     * @return AuthResponseDto with access and refresh tokens
     * @throws OAuth2AuthenticationException if OAuth2 profile mapping fails
     */
    AuthResponseDto handleGoogleOAuth2Login(String email, String name, String avatarUrl);

    /**
     * Validate a JWT access token.
     *
     * @param token the JWT token to validate
     * @return true if valid, false otherwise
     */
    boolean validateToken(String token);

    /**
     * Refresh an expired access token using a refresh token.
     *
     * @param refreshTokenDto containing the refresh token
     * @return AuthResponseDto with new access token
     * @throws TokenRefreshException if refresh token is invalid or expired
     */
    AuthResponseDto refreshToken(RefreshTokenDto refreshTokenDto);

    /**
     * Logout a user by invalidating their session.
     *
     * @param token the access token to invalidate
     */
    void logout(String token);

    /**
     * Get user by their unique ID.
     *
     * @param userId the user's ID
     * @return User object
     * @throws ResourceNotFoundException if user not found
     */
    User getUserById(Long userId);

    /**
     * Get user by their email address.
     *
     * @param email the user's email
     * @return User object
     * @throws ResourceNotFoundException if user not found
     */
    User getUserByEmail(String email);

    /**
     * Update user profile information.
     *
     * @param userId the user's ID (verified to prevent cross-user access)
     * @param profileUpdateDto the updated profile data
     * @return UserProfileDto with updated information
     * @throws ResourceNotFoundException if user not found
     */
    UserProfileDto updateProfile(Long userId, ProfileUpdateDto profileUpdateDto);

    /**
     * Change user password with validation of current password.
     *
     * @param userId the user's ID
     * @param passwordChangeDto containing current and new passwords
     * @throws InvalidCredentialsException if current password is incorrect
     * @throws ResourceNotFoundException if user not found
     * @throws InvalidCredentialsException if user is OAuth2-only and has no password
     */
    void changePassword(Long userId, PasswordChangeDto passwordChangeDto);

    /**
     * Update the user's currency preference.
     * Called by downstream services to sync currency changes.
     *
     * @param userId the user's ID
     * @param currency the new currency code (ISO 4217)
     * @throws ResourceNotFoundException if user not found
     */
    void updateCurrency(Long userId, String currency);

    /**
     * Soft-delete a user account (set isActive to false).
     * User cannot log in after deactivation but data is preserved.
     *
     * @param userId the user's ID to deactivate
     * @throws ResourceNotFoundException if user not found
     */
    void deactivateAccount(Long userId);

    /**
     * Get the user profile as a DTO (safe for API responses).
     *
     * @param userId the user's ID
     * @return UserProfileDto
     * @throws ResourceNotFoundException if user not found
     */
    UserProfileDto getUserProfile(Long userId);
}

