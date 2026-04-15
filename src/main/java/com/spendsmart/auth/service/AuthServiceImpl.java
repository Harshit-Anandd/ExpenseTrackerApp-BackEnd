/**
 * AuthServiceImpl implements all authentication and user management business logic.
 *
 * <p>This service handles:
 * - User registration with password hashing
 * - Login with credential validation
 * - OAuth2 user creation/update
 * - JWT token generation and validation
 * - Profile updates with security checks
 * - Password changes with current password verification
 * - Account deactivation (soft delete)
 *
 * <p>All entity data is mapped to DTOs before returning to the controller to prevent
 * exposing sensitive information (like password hashes) to the client.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.service;

import com.spendsmart.auth.dto.*;
import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.exception.*;
import com.spendsmart.auth.repository.UserRepository;
import com.spendsmart.auth.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Register a new user with validation.
     * Prevents duplicate emails and stores hashed password.
     */
    @Override
    public AuthResponseDto register(RegisterDto registerDto) {
        log.info("Registering new user with email: {}", registerDto.getEmail());

        // Validate email uniqueness
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            log.warn("Registration failed: email already exists: {}", registerDto.getEmail());
            throw new UserAlreadyExistsException(
                    "Email already registered. Please use a different email or try logging in."
            );
        }

        // Validate password confirmation
        if (!registerDto.getPassword().equals(registerDto.getPasswordConfirm())) {
            log.warn("Registration failed: password confirmation mismatch for {}", registerDto.getEmail());
            throw new InvalidCredentialsException("Passwords do not match. Please verify and try again.");
        }

        // Create new user entity
        User user = User.builder()
                .fullName(registerDto.getFullName())
                .email(registerDto.getEmail())
                .passwordHash(passwordEncoder.encode(registerDto.getPassword()))
                .provider(User.Provider.LOCAL)
                .role(User.Role.USER)
                .isActive(true)
                .currency("USD")
                .timezone("UTC")
                .monthlyBudget(5000.0)
                .build();

        // Save to database
        user = userRepository.save(user);
        log.info("User registered successfully: {} (ID: {})", user.getEmail(), user.getUserId());

        // Generate tokens and return response
        return generateAuthResponse(user);
    }

    /**
     * Authenticate user with email and password.
     * Validates credentials and checks account status.
     */
    @Override
    public AuthResponseDto login(LoginDto loginDto) {
        log.info("Login attempt for email: {}", loginDto.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found: {}", loginDto.getEmail());
                    return new InvalidCredentialsException("Invalid email or password.");
                });

        // Check if account is active
        if (!user.getIsActive()) {
            log.warn("Login failed: account deactivated: {}", loginDto.getEmail());
            throw new DeactivatedAccountException(
                    "Your account has been deactivated. Contact support to reactivate it."
            );
        }

        // Validate password
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password for: {}", loginDto.getEmail());
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        log.info("User logged in successfully: {}", loginDto.getEmail());
        return generateAuthResponse(user);
    }

    /**
     * Handle Google OAuth2 login.
     * Creates user if first time, updates profile if returning.
     */
    @Override
    public AuthResponseDto handleGoogleOAuth2Login(String email, String name, String avatarUrl) {
        log.info("OAuth2 login attempt for Google user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // First-time Google login - create new user
                    log.info("Creating new user from Google OAuth2: {}", email);
                    User newUser = User.builder()
                            .fullName(name)
                            .email(email)
                            .avatarUrl(avatarUrl)
                            .provider(User.Provider.GOOGLE)
                            .role(User.Role.USER)
                            .isActive(true)
                            .currency("USD")
                            .timezone("UTC")
                            .monthlyBudget(5000.0)
                            // No password for Google-only accounts
                            .build();
                    return userRepository.save(newUser);
                });

        // Returning Google user - update profile picture if changed
        if (user.getProvider() == User.Provider.GOOGLE && avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
            user.setFullName(name);
            user = userRepository.save(user);
            log.info("Updated Google user profile: {}", email);
        }

        // Check if account is active
        if (!user.getIsActive()) {
            log.warn("OAuth2 login failed: account deactivated: {}", email);
            throw new DeactivatedAccountException(
                    "Your account has been deactivated. Contact support to reactivate it."
            );
        }

        log.info("Google OAuth2 login successful: {}", email);
        return generateAuthResponse(user);
    }

    /**
     * Validate a JWT token.
     */
    @Override
    public boolean validateToken(String token) {
        return jwtUtils.validateToken(token);
    }

    /**
     * Refresh access token using refresh token.
     */
    @Override
    public AuthResponseDto refreshToken(RefreshTokenDto refreshTokenDto) {
        log.info("Token refresh attempt");

        String refreshToken = refreshTokenDto.getRefreshToken();

        // Validate refresh token
        if (!jwtUtils.validateToken(refreshToken)) {
            log.warn("Token refresh failed: invalid or expired refresh token");
            throw new TokenRefreshException("Refresh token is invalid or expired. Please log in again.");
        }

        // Verify it's actually a refresh token
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            log.warn("Token refresh failed: token is not a refresh token");
            throw new TokenRefreshException("Invalid token type. Expected a refresh token.");
        }

        // Extract user info and get user from database
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        User user = getUserById(userId);

        log.info("Token refreshed successfully for user: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    /**
     * Logout user by invalidating token.
     * In a stateless JWT setup, this is mainly for logging.
     * Optionally, tokens could be added to a blocklist.
     */
    @Override
    public void logout(String token) {
        log.info("User logout: token invalidated");
        // In a stateless JWT architecture, we log the logout.
        // For enhanced security, the token could be added to a Redis blocklist
        // with expiration equal to token TTL. For now, we rely on token expiration.
    }

    /**
     * Get user by ID.
     */
    @Override
    public User getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("User with ID %d not found.", userId)
                ));
    }

    /**
     * Get user by email.
     */
    @Override
    public User getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("User with email %s not found.", email)
                ));
    }

    /**
     * Update user profile with security checks.
     */
    @Override
    public UserProfileDto updateProfile(Long userId, ProfileUpdateDto profileUpdateDto) {
        log.info("Updating profile for user ID: {}", userId);

        User user = getUserById(userId);

        // Update fields if provided
        if (profileUpdateDto.getFullName() != null) {
            user.setFullName(profileUpdateDto.getFullName());
            log.debug("Updated fullName for user: {}", userId);
        }

        if (profileUpdateDto.getEmail() != null) {
            // Check if new email is already in use
            if (!user.getEmail().equals(profileUpdateDto.getEmail()) &&
                    userRepository.existsByEmail(profileUpdateDto.getEmail())) {
                log.warn("Email update failed: email already in use: {}", profileUpdateDto.getEmail());
                throw new UserAlreadyExistsException("This email is already registered.");
            }
            user.setEmail(profileUpdateDto.getEmail());
            log.debug("Updated email for user: {}", userId);
        }

        if (profileUpdateDto.getAvatarUrl() != null) {
            user.setAvatarUrl(profileUpdateDto.getAvatarUrl());
            log.debug("Updated avatarUrl for user: {}", userId);
        }

        if (profileUpdateDto.getTimezone() != null) {
            user.setTimezone(profileUpdateDto.getTimezone());
            log.debug("Updated timezone for user: {}", userId);
        }

        if (profileUpdateDto.getCurrency() != null) {
            user.setCurrency(profileUpdateDto.getCurrency());
            log.debug("Updated currency for user: {}", userId);
        }

        if (profileUpdateDto.getMonthlyBudget() != null) {
            user.setMonthlyBudget(profileUpdateDto.getMonthlyBudget());
            log.debug("Updated monthlyBudget for user: {}", userId);
        }

        user = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", userId);
        return mapUserToProfileDto(user);
    }

    /**
     * Change password with current password validation.
     */
    @Override
    public void changePassword(Long userId, PasswordChangeDto passwordChangeDto) {
        log.info("Password change attempt for user ID: {}", userId);

        User user = getUserById(userId);

        // Check if user is OAuth2-only (no password)
        if (user.getProvider() == User.Provider.GOOGLE && user.getPasswordHash() == null) {
            log.warn("Password change failed: user is OAuth2-only: {}", userId);
            throw new InvalidCredentialsException(
                    "Your account uses Google authentication. You cannot set a password via this endpoint."
            );
        }

        // Validate current password
        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Password change failed: current password incorrect for user: {}", userId);
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        // Validate new password confirmation
        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getNewPasswordConfirm())) {
            log.warn("Password change failed: new password confirmation mismatch for user: {}", userId);
            throw new InvalidCredentialsException("New passwords do not match.");
        }

        // Prevent setting same password
        if (passwordChangeDto.getCurrentPassword().equals(passwordChangeDto.getNewPassword())) {
            log.warn("Password change failed: new password same as current for user: {}", userId);
            throw new InvalidCredentialsException("New password must be different from current password.");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(passwordChangeDto.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", userId);
    }

    /**
     * Update currency for the user.
     */
    @Override
    public void updateCurrency(Long userId, String currency) {
        log.info("Updating currency for user ID: {} to: {}", userId, currency);

        User user = getUserById(userId);
        user.setCurrency(currency);
        userRepository.save(user);

        log.info("Currency updated successfully for user: {}", userId);
    }

    /**
     * Deactivate user account (soft delete).
     */
    @Override
    public void deactivateAccount(Long userId) {
        log.info("Deactivating account for user ID: {}", userId);

        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);

        log.info("Account deactivated successfully for user: {}", userId);
    }

    /**
     * Get user profile as DTO (safe for API responses).
     */
    @Override
    public UserProfileDto getUserProfile(Long userId) {
        log.debug("Fetching user profile for user ID: {}", userId);
        User user = getUserById(userId);
        return mapUserToProfileDto(user);
    }

    /**
     * Generate authentication response with tokens.
     *
     * @param user the authenticated user
     * @return AuthResponseDto with tokens and user info
     */
    private AuthResponseDto generateAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getUserId(), user.getEmail());

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .expiresIn((long) (24 * 60 * 60)) // 24 hours in seconds
                .build();
    }

    /**
     * Map User entity to UserProfileDto (excludes sensitive fields).
     *
     * @param user the user entity
     * @return UserProfileDto for API responses
     */
    private UserProfileDto mapUserToProfileDto(User user) {
        return UserProfileDto.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .currency(user.getCurrency())
                .timezone(user.getTimezone())
                .provider(user.getProvider().name())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .monthlyBudget(user.getMonthlyBudget())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

