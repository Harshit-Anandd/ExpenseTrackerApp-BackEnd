/**
 * JwtAccessDeniedHandler handles cases where an authenticated user attempts to
 * access a resource they don't have permission for.
 *
 * <p>Returns a 403 Forbidden response with JSON error details instead of
 * the default Spring Security error page.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsmart.auth.exception.ErrorResponseDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * Handle access denied for authenticated but unauthorized users.
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn("Access denied for user on {}: {}", request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponseDto error = ErrorResponseDto.builder()
                .status(HttpServletResponse.SC_FORBIDDEN)
                .error("FORBIDDEN")
                .message("You do not have permission to access this resource.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(error));
    }
}

