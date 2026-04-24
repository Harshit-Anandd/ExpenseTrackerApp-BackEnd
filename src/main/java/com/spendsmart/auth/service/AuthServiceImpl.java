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
import com.spendsmart.auth.messaging.AuthEventPublisher;
import com.spendsmart.auth.repository.UserRepository;
import com.spendsmart.auth.security.JwtUtils;
import com.spendsmart.auth.security.TokenBlocklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String SEVERITY_INFO = "INFO";
    private static final String SEVERITY_WARNING = "WARNING";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenBlocklistService tokenBlocklistService;

    @Autowired
    private OtpChallengeService otpChallengeService;

    @Autowired
    private AuthEventPublisher authEventPublisher;

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
                .emailVerified(false)
                .twoFactorEnabled(false)
                .currency("USD")
                .timezone("UTC")
                .build();

        // Save to database
        user = userRepository.save(user);
        log.info("User registered successfully: {} (ID: {})", user.getEmail(), user.getUserId());

        return issueOtpChallengeResponse(user, OtpPurpose.SIGNUP,
                "Verify your email with the OTP sent to complete signup.");
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

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            log.info("Login requires signup verification OTP for user: {}", loginDto.getEmail());
            return issueOtpChallengeResponse(user, OtpPurpose.SIGNUP,
                    "Verify your email with OTP before logging in.");
        }

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            log.info("Login requires 2FA OTP for user: {}", loginDto.getEmail());
            return issueOtpChallengeResponse(user, OtpPurpose.LOGIN_2FA,
                    "Enter the OTP sent to your email to complete login.");
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
                            .emailVerified(true)
                            .currency("USD")
                            .timezone("UTC")
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

        if (tokenBlocklistService.isRevoked(refreshToken)) {
            log.warn("Token refresh failed: refresh token already revoked/reused");
            throw new TokenRefreshException("Refresh token has already been used. Please log in again.");
        }

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

        if (!user.getIsActive()) {
            log.warn("Token refresh failed: account deactivated: {}", user.getEmail());
            throw new DeactivatedAccountException(
                    "Your account has been deactivated. Contact support to reactivate it."
            );
        }

        // Single-use refresh tokens: revoke the old refresh token until its natural expiration.
        long ttlMillis = jwtUtils.getTimeRemainingInToken(refreshToken);
        tokenBlocklistService.revokeToken(refreshToken, ttlMillis);

        log.info("Token refreshed successfully for user: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    @Override
    public AuthResponseDto verifyOtp(OtpVerificationDto otpVerificationDto) {
        OtpPurpose purpose;
        try {
            purpose = OtpPurpose.from(otpVerificationDto.getPurpose());
        } catch (Exception ex) {
            throw new InvalidCredentialsException("Invalid OTP purpose.");
        }

        boolean valid = otpChallengeService.verify(
                otpVerificationDto.getEmail(),
                purpose,
                otpVerificationDto.getChallengeId(),
                otpVerificationDto.getOtpCode()
        );

        if (!valid) {
            throw new InvalidCredentialsException("Invalid or expired OTP.");
        }

        User user = getUserByEmail(otpVerificationDto.getEmail());
        if (!user.getIsActive()) {
            throw new DeactivatedAccountException(
                    "Your account has been deactivated. Contact support to reactivate it."
            );
        }

        if (purpose == OtpPurpose.SIGNUP && !Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            userRepository.save(user);
            log.info("Email verified for user: {}", user.getEmail());
        }

        return generateAuthResponse(user);
    }

    @Override
    public AuthResponseDto resendOtp(OtpResendDto otpResendDto) {
        User user = getUserByEmail(otpResendDto.getEmail());

        OtpPurpose purpose;
        try {
            purpose = OtpPurpose.from(otpResendDto.getPurpose());
        } catch (Exception ex) {
            throw new InvalidCredentialsException("Invalid OTP purpose.");
        }

        if (purpose == OtpPurpose.SIGNUP && Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new InvalidCredentialsException("Email is already verified.");
        }

        if (purpose == OtpPurpose.LOGIN_2FA && !Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new InvalidCredentialsException("Two-factor authentication is not enabled for this account.");
        }

        return issueOtpChallengeResponse(user, purpose, "A new OTP has been sent.");
    }

    /**
     * Logout user by revoking access token and optional refresh token.
     */
    @Override
    public void logout(String accessToken, String refreshToken) {
        revokeIfValid(accessToken, false, "access");
        revokeIfValid(refreshToken, true, "refresh");
    }

    private void revokeIfValid(String token, boolean requireRefreshType, String tokenLabel) {
        if (token == null || token.isBlank()) {
            if (!requireRefreshType) {
                log.warn("Logout requested without {} token", tokenLabel);
            }
            return;
        }

        if (!jwtUtils.validateToken(token)) {
            log.warn("Logout requested with invalid/expired {} token", tokenLabel);
            return;
        }

        if (requireRefreshType && !jwtUtils.isRefreshToken(token)) {
            log.warn("Logout requested with non-refresh token in refresh slot");
            return;
        }

        long ttlMillis = jwtUtils.getTimeRemainingInToken(token);
        tokenBlocklistService.revokeToken(token, ttlMillis);
        log.info("User logout: {} token revoked for {} ms", tokenLabel, ttlMillis);
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

    @Override
    public void setTwoFactor(Long userId, boolean enabled) {
        User user = getUserById(userId);

        if (user.getProvider() != User.Provider.LOCAL) {
            throw new InvalidCredentialsException("2FA can only be managed for local-authenticated accounts.");
        }

        if (enabled && !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new InvalidCredentialsException("Verify your email before enabling 2FA.");
        }

        user.setTwoFactorEnabled(enabled);
        userRepository.save(user);
        log.info("2FA updated for user {}: {}", user.getEmail(), enabled);
        authEventPublisher.publish(
                "TWO_FACTOR_UPDATED",
                user.getUserId(),
                user.getEmail(),
                "Two-factor authentication updated",
                enabled ? "Two-factor authentication has been enabled." : "Two-factor authentication has been disabled.",
                SEVERITY_INFO,
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<AdminUserDto> getAllUsersForAdmin(String query, Boolean active, String role, String subscriptionType) {
        java.util.List<User> users;
        if (query != null && !query.isBlank()) {
            users = userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(query.trim(), query.trim());
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .filter(user -> active == null || active.equals(user.getIsActive()))
                .filter(user -> role == null || role.isBlank() || user.getRole().name().equalsIgnoreCase(role))
                .filter(user -> subscriptionType == null || subscriptionType.isBlank() || user.getSubscriptionType().name().equalsIgnoreCase(subscriptionType))
                .map(this::mapUserToAdminDto)
                .toList();
    }

    @Override
    public AdminUserDto updateUserStatus(Long userId, boolean active) {
        User user = getUserById(userId);
        user.setIsActive(active);
        user = userRepository.save(user);

        authEventPublisher.publish(
                "ADMIN_USER_STATUS_CHANGED",
                user.getUserId(),
                user.getEmail(),
                "Account status updated",
                active ? "Your account has been activated by an administrator."
                        : "Your account has been suspended by an administrator.",
                active ? SEVERITY_INFO : SEVERITY_WARNING,
                null
        );

        return mapUserToAdminDto(user);
    }

    @Override
    public AdminUserDto updateUserRole(Long userId, String role) {
        User user = getUserById(userId);
        User.Role parsedRole;
        try {
            parsedRole = User.Role.valueOf(role.trim().toUpperCase());
        } catch (Exception ex) {
            throw new InvalidCredentialsException("Invalid role value. Allowed values: USER, ADMIN.");
        }

        user.setRole(parsedRole);
        user = userRepository.save(user);

        authEventPublisher.publish(
                "ADMIN_USER_ROLE_CHANGED",
                user.getUserId(),
                user.getEmail(),
                "Role updated",
                "Your role has been updated to " + parsedRole.name() + ".",
                SEVERITY_INFO,
                null
        );

        return mapUserToAdminDto(user);
    }

    @Override
    public AdminUserDto updateUserSubscription(Long userId, String subscriptionType) {
        User user = getUserById(userId);
        User.SubscriptionType parsedSubscription;
        try {
            parsedSubscription = User.SubscriptionType.valueOf(subscriptionType.trim().toUpperCase());
        } catch (Exception ex) {
            throw new InvalidCredentialsException("Invalid subscriptionType value. Allowed values: NORMAL, PAID.");
        }

        user.setSubscriptionType(parsedSubscription);
        user = userRepository.save(user);

        authEventPublisher.publish(
                "ADMIN_USER_SUBSCRIPTION_CHANGED",
                user.getUserId(),
                user.getEmail(),
                "Subscription updated",
                "Your subscription has been updated to " + parsedSubscription.name() + ".",
                SEVERITY_INFO,
                null
        );

        return mapUserToAdminDto(user);
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
        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getEmail(), user.getRole().name(), user.getSubscriptionType().name());
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
                .requiresOtp(false)
                .build();
    }

    private AuthResponseDto issueOtpChallengeResponse(User user, OtpPurpose purpose, String message) {
        OtpChallenge challenge = otpChallengeService.createChallenge(user.getEmail(), purpose);

        authEventPublisher.publish(
                "OTP_CHALLENGE_CREATED",
                user.getUserId(),
                user.getEmail(),
                "OTP verification required",
                "A one-time password was generated for " + purpose.name() + ".",
                SEVERITY_INFO,
                challenge.otpCode()
        );

        return AuthResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .requiresOtp(true)
                .otpPurpose(purpose.name())
                .otpChallengeId(challenge.challengeId())
                .otpExpiresInSeconds(challenge.expiresInSeconds())
                .message(message)
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
                .subscriptionType(user.getSubscriptionType().name())
                .emailVerified(user.getEmailVerified())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private AdminUserDto mapUserToAdminDto(User user) {
        return AdminUserDto.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .subscriptionType(user.getSubscriptionType().name())
                .isActive(user.getIsActive())
                .provider(user.getProvider().name())
                .emailVerified(user.getEmailVerified())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

