/**
 * AuthServiceImplTest contains comprehensive unit tests for the AuthServiceImpl.
 *
 * <p>Tests cover:
 * - User registration (success, duplicate email, password mismatch)
 * - User login (success, invalid email, invalid password, deactivated account)
 * - Google OAuth2 login (new user, returning user)
 * - Password change (success, invalid current password, OAuth2-only user)
 * - Token validation
 * - Profile updates
 * - Account deactivation
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenBlocklistService tokenBlocklistService;

    @Mock
    private OtpChallengeService otpChallengeService;

    @Mock
    private AuthEventPublisher authEventPublisher;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterDto registerDto;
    private LoginDto loginDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .passwordHash("hashedPassword")
                .currency("USD")
                .timezone("UTC")
                .provider(User.Provider.LOCAL)
                .role(User.Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build();

        registerDto = RegisterDto.builder()
                .fullName("Jane Doe")
                .email("jane@example.com")
                .password("password123")
                .passwordConfirm("password123")
                .build();

        loginDto = LoginDto.builder()
                .email("john@example.com")
                .password("password123")
                .build();
    }

    // ========== Registration Tests ==========

    @Test
    @DisplayName("Should register a new user successfully")
    void testRegisterSuccess() {
        // Arrange
        when(userRepository.existsByEmail(registerDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerDto.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(otpChallengeService.createChallenge(testUser.getEmail(), OtpPurpose.SIGNUP))
                .thenReturn(new OtpChallenge("signupChallenge", 300L, "123456"));

        // Act
        AuthResponseDto response = authService.register(registerDto);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getUserId(), response.getUserId());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertTrue(response.getRequiresOtp());
        assertEquals("SIGNUP", response.getOtpPurpose());
        verify(userRepository).existsByEmail(registerDto.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail to register with duplicate email")
    void testRegisterDuplicateEmail() {
        // Arrange
        when(userRepository.existsByEmail(registerDto.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerDto));
        verify(userRepository).existsByEmail(registerDto.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail to register with password mismatch")
    void testRegisterPasswordMismatch() {
        // Arrange
        registerDto.setPasswordConfirm("different");

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.register(registerDto));
    }

    // ========== Login Tests ==========

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        // Arrange
        when(userRepository.findByEmail(loginDto.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginDto.getPassword(), testUser.getPasswordHash())).thenReturn(true);
        when(jwtUtils.generateAccessToken(testUser.getUserId(), testUser.getEmail(), "USER", "NORMAL"))
                .thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken(testUser.getUserId(), testUser.getEmail()))
                .thenReturn("refreshToken");

        // Act
        AuthResponseDto response = authService.login(loginDto);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getUserId(), response.getUserId());
        assertEquals("accessToken", response.getAccessToken());
        verify(userRepository).findByEmail(loginDto.getEmail());
    }

    @Test
    @DisplayName("Should fail to login with non-existent email")
    void testLoginInvalidEmail() {
        // Arrange
        when(userRepository.findByEmail(loginDto.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginDto));
    }

    @Test
    @DisplayName("Should fail to login with invalid password")
    void testLoginInvalidPassword() {
        // Arrange
        when(userRepository.findByEmail(loginDto.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginDto.getPassword(), testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginDto));
    }

    @Test
    @DisplayName("Should fail to login with deactivated account")
    void testLoginDeactivatedAccount() {
        // Arrange
        testUser.setIsActive(false);
        when(userRepository.findByEmail(loginDto.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(DeactivatedAccountException.class, () -> authService.login(loginDto));
    }

    @Test
    @DisplayName("Should require signup OTP for unverified email login")
    void testLoginRequiresSignupOtpWhenEmailNotVerified() {
        testUser.setEmailVerified(false);
        when(userRepository.findByEmail(loginDto.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginDto.getPassword(), testUser.getPasswordHash())).thenReturn(true);
        when(otpChallengeService.createChallenge(testUser.getEmail(), OtpPurpose.SIGNUP))
                .thenReturn(new OtpChallenge("signupChallenge", 300L, "123456"));

        AuthResponseDto response = authService.login(loginDto);

        assertTrue(response.getRequiresOtp());
        assertEquals("SIGNUP", response.getOtpPurpose());
        assertNull(response.getAccessToken());
    }

    @Test
    @DisplayName("Should require 2FA OTP when two-factor is enabled")
    void testLoginRequiresTwoFactorOtpWhenEnabled() {
        testUser.setTwoFactorEnabled(true);
        when(userRepository.findByEmail(loginDto.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginDto.getPassword(), testUser.getPasswordHash())).thenReturn(true);
        when(otpChallengeService.createChallenge(testUser.getEmail(), OtpPurpose.LOGIN_2FA))
                .thenReturn(new OtpChallenge("twoFactorChallenge", 300L, "654321"));

        AuthResponseDto response = authService.login(loginDto);

        assertTrue(response.getRequiresOtp());
        assertEquals("LOGIN_2FA", response.getOtpPurpose());
        assertNull(response.getAccessToken());
    }

    // ========== Google OAuth2 Tests ==========

    @Test
    @DisplayName("Should create new user on first Google OAuth2 login")
    void testGoogleOAuth2FirstLogin() {
        // Arrange
        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        User newGoogleUser = User.builder()
                .userId(2L)
                .fullName("Google User")
                .email("newuser@gmail.com")
                .provider(User.Provider.GOOGLE)
                .role(User.Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(newGoogleUser);
        when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken(anyLong(), anyString())).thenReturn("refreshToken");

        // Act
        AuthResponseDto response = authService.handleGoogleOAuth2Login("newuser@gmail.com", "Google User", "https://avatar.jpg");

        // Assert
        assertNotNull(response);
        assertEquals("USER", response.getRole());
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle returning Google OAuth2 user")
    void testGoogleOAuth2ReturningLogin() {
        // Arrange
        testUser.setProvider(User.Provider.GOOGLE);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(jwtUtils.generateAccessToken(testUser.getUserId(), testUser.getEmail(), "USER", "NORMAL"))
                .thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken(testUser.getUserId(), testUser.getEmail()))
                .thenReturn("refreshToken");

        // Act
        AuthResponseDto response = authService.handleGoogleOAuth2Login(testUser.getEmail(), "Updated Name", "https://newavatar.jpg");

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getUserId(), response.getUserId());
        verify(userRepository).save(testUser);
    }

    // ========== Password Change Tests ==========

    @Test
    @DisplayName("Should change password successfully")
    void testChangePasswordSuccess() {
        // Arrange
        when(userRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");

        PasswordChangeDto dto = PasswordChangeDto.builder()
                .currentPassword("password123")
                .newPassword("newPassword123")
                .newPasswordConfirm("newPassword123")
                .build();

        // Act
        assertDoesNotThrow(() -> authService.changePassword(testUser.getUserId(), dto));

        // Assert
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should fail password change with incorrect current password")
    void testChangePasswordIncorrectCurrent() {
        // Arrange
        when(userRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPasswordHash())).thenReturn(false);

        PasswordChangeDto dto = PasswordChangeDto.builder()
                .currentPassword("wrongPassword")
                .newPassword("newPassword123")
                .newPasswordConfirm("newPassword123")
                .build();

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () ->
                authService.changePassword(testUser.getUserId(), dto));
    }

    @Test
    @DisplayName("Should fail password change for OAuth2-only user")
    void testChangePasswordOAuth2OnlyUser() {
        // Arrange
        testUser.setProvider(User.Provider.GOOGLE);
        testUser.setPasswordHash(null);
        when(userRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testUser));

        PasswordChangeDto dto = PasswordChangeDto.builder()
                .currentPassword("password123")
                .newPassword("newPassword123")
                .newPasswordConfirm("newPassword123")
                .build();

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () ->
                authService.changePassword(testUser.getUserId(), dto));
    }

    // ========== Account Deactivation Tests ==========

    @Test
    @DisplayName("Should deactivate account successfully")
    void testDeactivateAccountSuccess() {
        // Arrange
        when(userRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testUser));

        // Act
        authService.deactivateAccount(testUser.getUserId());

        // Assert
        assertFalse(testUser.getIsActive());
        verify(userRepository).save(testUser);
    }

    // ========== Token Validation Tests ==========

    @Test
    @DisplayName("Should revoke token on logout when token is valid")
    void testLogoutRevokesToken() {
        String accessToken = "validAccessToken";
        when(jwtUtils.validateToken(accessToken)).thenReturn(true);
        when(jwtUtils.getTimeRemainingInToken(accessToken)).thenReturn(30_000L);

        authService.logout(accessToken, null);

        verify(tokenBlocklistService).revokeToken(accessToken, 30_000L);
    }

    @Test
    @DisplayName("Should ignore logout when token is invalid")
    void testLogoutInvalidToken() {
        String accessToken = "invalidAccessToken";
        when(jwtUtils.validateToken(accessToken)).thenReturn(false);

        authService.logout(accessToken, null);

        verify(tokenBlocklistService, never()).revokeToken(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should ignore logout when token is blank")
    void testLogoutBlankToken() {
        authService.logout(" ", null);

        verify(jwtUtils, never()).validateToken(anyString());
        verify(tokenBlocklistService, never()).revokeToken(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should revoke both access and refresh tokens on logout")
    void testLogoutRevokesAccessAndRefreshTokens() {
        String accessToken = "validAccessToken";
        String refreshToken = "validRefreshToken";

        when(jwtUtils.validateToken(accessToken)).thenReturn(true);
        when(jwtUtils.getTimeRemainingInToken(accessToken)).thenReturn(30_000L);
        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtils.getTimeRemainingInToken(refreshToken)).thenReturn(120_000L);

        authService.logout(accessToken, refreshToken);

        verify(tokenBlocklistService).revokeToken(accessToken, 30_000L);
        verify(tokenBlocklistService).revokeToken(refreshToken, 120_000L);
    }

    @Test
    @DisplayName("Should ignore non-refresh token in logout refresh slot")
    void testLogoutIgnoresInvalidRefreshSlotToken() {
        String accessToken = "validAccessToken";
        String invalidRefreshSlotToken = "someAccessToken";

        when(jwtUtils.validateToken(accessToken)).thenReturn(true);
        when(jwtUtils.getTimeRemainingInToken(accessToken)).thenReturn(30_000L);
        when(jwtUtils.validateToken(invalidRefreshSlotToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(invalidRefreshSlotToken)).thenReturn(false);

        authService.logout(accessToken, invalidRefreshSlotToken);

        verify(tokenBlocklistService).revokeToken(accessToken, 30_000L);
        verify(tokenBlocklistService, never()).revokeToken(eq(invalidRefreshSlotToken), anyLong());
    }

    @Test
    @DisplayName("Should validate token successfully")
    void testValidateTokenSuccess() {
        // Arrange
        String token = "validToken";
        when(jwtUtils.validateToken(token)).thenReturn(true);

        // Act
        boolean result = authService.validateToken(token);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should reject invalid token")
    void testValidateTokenFailure() {
        // Arrange
        String token = "invalidToken";
        when(jwtUtils.validateToken(token)).thenReturn(false);

        // Act
        boolean result = authService.validateToken(token);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should refresh token and revoke used refresh token")
    void testRefreshTokenSuccessRevokesUsedToken() {
        RefreshTokenDto refreshTokenDto = RefreshTokenDto.builder()
                .refreshToken("validRefreshToken")
                .build();

        when(tokenBlocklistService.isRevoked("validRefreshToken")).thenReturn(false);
        when(jwtUtils.validateToken("validRefreshToken")).thenReturn(true);
        when(jwtUtils.isRefreshToken("validRefreshToken")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("validRefreshToken")).thenReturn(1L);
        when(jwtUtils.getTimeRemainingInToken("validRefreshToken")).thenReturn(120_000L);
        when(userRepository.findByUserId(1L)).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateAccessToken(testUser.getUserId(), testUser.getEmail(), "USER", "NORMAL"))
                .thenReturn("newAccessToken");
        when(jwtUtils.generateRefreshToken(testUser.getUserId(), testUser.getEmail()))
                .thenReturn("newRefreshToken");

        AuthResponseDto response = authService.refreshToken(refreshTokenDto);

        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("newRefreshToken", response.getRefreshToken());
        verify(tokenBlocklistService).revokeToken("validRefreshToken", 120_000L);
    }

    @Test
    @DisplayName("Should fail refresh when token is already revoked")
    void testRefreshTokenFailsWhenAlreadyRevoked() {
        RefreshTokenDto refreshTokenDto = RefreshTokenDto.builder()
                .refreshToken("revokedRefreshToken")
                .build();

        when(tokenBlocklistService.isRevoked("revokedRefreshToken")).thenReturn(true);

        assertThrows(TokenRefreshException.class, () -> authService.refreshToken(refreshTokenDto));

        verify(jwtUtils, never()).validateToken(anyString());
        verify(tokenBlocklistService, never()).revokeToken(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should verify signup OTP and return auth tokens")
    void testVerifyOtpForSignup() {
        testUser.setEmailVerified(false);
        OtpVerificationDto dto = OtpVerificationDto.builder()
                .email(testUser.getEmail())
                .otpCode("123456")
                .purpose("SIGNUP")
                .challengeId("challenge")
                .build();

        when(otpChallengeService.verify(testUser.getEmail(), OtpPurpose.SIGNUP, "challenge", "123456"))
                .thenReturn(true);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(jwtUtils.generateAccessToken(testUser.getUserId(), testUser.getEmail(), "USER", "NORMAL"))
                .thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken(testUser.getUserId(), testUser.getEmail()))
                .thenReturn("refreshToken");

        AuthResponseDto response = authService.verifyOtp(dto);

        assertFalse(response.getRequiresOtp());
        assertEquals("accessToken", response.getAccessToken());
        assertTrue(testUser.getEmailVerified());
    }

    @Test
    @DisplayName("Should fail verify OTP for invalid code")
    void testVerifyOtpInvalidCode() {
        OtpVerificationDto dto = OtpVerificationDto.builder()
                .email(testUser.getEmail())
                .otpCode("000000")
                .purpose("LOGIN_2FA")
                .challengeId("challenge")
                .build();

        when(otpChallengeService.verify(testUser.getEmail(), OtpPurpose.LOGIN_2FA, "challenge", "000000"))
                .thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.verifyOtp(dto));
    }

    @Test
    @DisplayName("Should resend OTP challenge")
    void testResendOtp() {
        OtpResendDto dto = OtpResendDto.builder()
                .email(testUser.getEmail())
                .purpose("LOGIN_2FA")
                .build();

        testUser.setTwoFactorEnabled(true);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(otpChallengeService.createChallenge(testUser.getEmail(), OtpPurpose.LOGIN_2FA))
                .thenReturn(new OtpChallenge("resendChallenge", 300L, "111222"));

        AuthResponseDto response = authService.resendOtp(dto);

        assertTrue(response.getRequiresOtp());
        assertEquals("LOGIN_2FA", response.getOtpPurpose());
    }

    @Test
    @DisplayName("Should enable 2FA for verified local user")
    void testSetTwoFactorEnabled() {
        when(userRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testUser));

        authService.setTwoFactor(testUser.getUserId(), true);

        assertTrue(testUser.getTwoFactorEnabled());
        verify(userRepository).save(testUser);
    }

    // ========== User Lookup Tests ==========

    @Test
    @DisplayName("Should get user by ID successfully")
    void testGetUserByIdSuccess() {
        // Arrange
        when(userRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testUser));

        // Act
        User result = authService.getUserById(testUser.getUserId());

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getUserId(), result.getUserId());
    }

    @Test
    @DisplayName("Should fail to get user by ID if not found")
    void testGetUserByIdNotFound() {
        // Arrange
        when(userRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> authService.getUserById(999L));
    }
}

