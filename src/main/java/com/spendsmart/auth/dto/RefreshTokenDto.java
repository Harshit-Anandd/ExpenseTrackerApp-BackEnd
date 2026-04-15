/**
 * RefreshTokenDto is the data transfer object for token refresh requests.
 *
 * <p>Contains the refresh token that should be exchanged for a new access token.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenDto {

    /**
     * Refresh token obtained during login.
     * Used to obtain a new access token without re-authenticating.
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}

