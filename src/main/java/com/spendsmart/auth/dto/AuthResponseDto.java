/**
 * AuthResponseDto is the data transfer object for successful authentication responses.
 *
 * <p>Returned by login and registration endpoints. Contains the user's access token,
 * refresh token, and essential user information. Password is never included.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    /**
     * JWT Access Token for authenticating subsequent API requests.
     * Valid for 24 hours by default.
     */
    private String accessToken;

    /**
     * Refresh Token for obtaining a new access token when the current one expires.
     * Valid for 7 days by default.
     */
    private String refreshToken;

    /**
     * Token type (typically "Bearer" for JWT).
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Unique identifier of the authenticated user.
     */
    private Long userId;

    /**
     * Email address of the authenticated user.
     */
    private String email;

    /**
     * Full name of the authenticated user.
     */
    private String fullName;

    /**
     * User's role in the system (USER or ADMIN).
     */
    private String role;

    /**
     * Time to live (in seconds) for the access token.
     */
    private Long expiresIn;
}

