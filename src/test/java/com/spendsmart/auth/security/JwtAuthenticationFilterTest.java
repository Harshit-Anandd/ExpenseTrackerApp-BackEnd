package com.spendsmart.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private TokenBlocklistService tokenBlocklistService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(filter, "tokenBlocklistService", tokenBlocklistService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate request with valid non-revoked token")
    void shouldAuthenticateWithValidToken() throws ServletException, IOException {
        String token = "validToken";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        Claims claims = mock(Claims.class);
        when(tokenBlocklistService.isRevoked(token)).thenReturn(false);
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getClaimsFromToken(token)).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(claims.getSubject()).thenReturn("john@example.com");
        when(claims.get("role")).thenReturn("USER");

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("john@example.com", authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> "ROLE_USER".equals(grantedAuthority.getAuthority())));
    }

    @Test
    @DisplayName("Should not authenticate request with revoked token")
    void shouldRejectRevokedToken() throws ServletException, IOException {
        String token = "revokedToken";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(tokenBlocklistService.isRevoked(token)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtils, never()).validateToken(anyString());
    }
}

