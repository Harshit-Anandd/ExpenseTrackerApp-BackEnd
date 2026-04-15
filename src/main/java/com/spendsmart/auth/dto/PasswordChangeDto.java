/**
 * PasswordChangeDto is the data transfer object for password change requests.
 *
 * <p>Requires the current password to verify user identity and the new password
 * to replace the old one. The new password must be confirmed to prevent typos.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordChangeDto {

    /**
     * Current password (for verification).
     * Must match the user's existing password hash.
     * Not applicable for OAuth2-only accounts.
     */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /**
     * New password to replace the old one.
     * Must be strong (at least 8 characters) and different from the current password.
     */
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 255, message = "New password must be between 8 and 255 characters")
    private String newPassword;

    /**
     * Confirmation of the new password.
     * Must match the newPassword field.
     */
    @NotBlank(message = "Password confirmation is required")
    private String newPasswordConfirm;
}

