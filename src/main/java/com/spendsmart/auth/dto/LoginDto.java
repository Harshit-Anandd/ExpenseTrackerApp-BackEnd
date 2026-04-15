/**
 * LoginDto is the data transfer object for user login requests.
 *
 * <p>Encapsulates email and password credentials for authentication.
 * Both fields are validated on the server side.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDto {

    /**
     * Email address of the user attempting to log in.
     * Must be a valid email format.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Password associated with the email.
     * Compared against the hashed password in the database.
     */
    @NotBlank(message = "Password is required")
    private String password;
}

