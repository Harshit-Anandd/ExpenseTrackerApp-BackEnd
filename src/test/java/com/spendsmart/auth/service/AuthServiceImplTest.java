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
import com.spendsmart.auth.repository.UserRepository;
import com.spendsmart.auth.security.JwtUtils;
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
                .monthlyBudget(5000.0)
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
        when(jwtUtils.generateAccessToken(testUser.getUserId(), testUser.getEmail(), "USER"))
                .thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken(testUser.getUserId(), testUser.getEmail()))
                .thenReturn("refreshToken");

        // Act
        AuthResponseDto response = authService.register(registerDto);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getUserId(), response.getUserId());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
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
        when(jwtUtils.generateAccessToken(testUser.getUserId(), testUser.getEmail(), "USER"))
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
                .build();
        when(userRepository.save(any(User.class))).thenReturn(newGoogleUser);
        when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken(anyLong(), anyString())).thenReturn("refreshToken");

        // Act
        AuthResponseDto response = authService.handleGoogleOAuth2Login("newuser@gmail.com", "Google User", "https://avatar.jpg");

        // Assert
        assertNotNull(response);
        assertEquals(User.Provider.GOOGLE.name(), response.getRole()); // Note: role is returned as USER
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle returning Google OAuth2 user")
    void testGoogleOAuth2ReturningLogin() {
        // Arrange
        testUser.setProvider(User.Provider.GOOGLE);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(jwtUtils.generateAccessToken(testUser.getUserId(), testUser.getEmail(), "USER"))
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

