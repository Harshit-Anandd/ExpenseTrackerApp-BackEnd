/**
 * RegisterDto is the data transfer object for user registration requests.
 *
 * <p>This DTO encapsulates the input validation for the registration endpoint.
 * All fields are validated on the server side using Jakarta Validation annotations.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.dto;

import jakarta.validation.constraints.Email;
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
public class RegisterDto {

    /**
     * Full name of the user registering.
     * Must not be blank and should be reasonably long.
     */
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    private String fullName;

    /**
     * Email address used for login and communication.
     * Must be a valid email format and unique across the system.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Password chosen by the user.
     * Must be strong and securely stored (hashed with BCrypt).
     * Not stored or returned in responses.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    private String password;

    /**
     * Confirmation password (must match password field).
     * Validated on the server side to ensure correctness.
     */
    @NotBlank(message = "Password confirmation is required")
    private String passwordConfirm;
}

