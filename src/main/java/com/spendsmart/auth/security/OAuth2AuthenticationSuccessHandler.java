package com.spendsmart.auth.security;

import com.spendsmart.auth.dto.AuthResponseDto;
import com.spendsmart.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private AuthService authService;

    @Value("${app.oauth2.redirect-success-url:http://localhost:5173/oauth/callback}")
    private String successRedirectUrl;

    @Value("${app.oauth2.redirect-failure-url:http://localhost:5173/login}")
    private String failureRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            response.sendRedirect(failureRedirectUrl + "?error=oauth_principal_missing");
            return;
        }

        String email = valueAsString(oAuth2User.getAttributes().get("email"));
        if (email == null || email.isBlank()) {
            response.sendRedirect(failureRedirectUrl + "?error=oauth_email_missing");
            return;
        }

        try {
            AuthResponseDto authResponse = authService.handleGoogleOAuth2Login(
                    email,
                    valueAsString(oAuth2User.getAttributes().get("name")),
                    valueAsString(oAuth2User.getAttributes().get("picture"))
            );

            String redirectUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                    .queryParam("accessToken", authResponse.getAccessToken())
                    .queryParam("refreshToken", authResponse.getRefreshToken())
                    .queryParam("role", authResponse.getRole())
                    .build(true)
                    .toUriString();

            response.sendRedirect(redirectUrl);
        } catch (Exception ex) {
            log.warn("OAuth2 success handler failed: {}", ex.getMessage());
            response.sendRedirect(failureRedirectUrl + "?error=oauth_login_failed");
        }
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
    }
}

