/**
 * ProfileUpdateDto is the data transfer object for user profile update requests.
 *
 * <p>Allows users to update their profile information such as name, email, avatar, timezone,
 * currency, and monthly budget. All fields are optional to support partial updates.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateDto {

    /**
     * Updated full name of the user (optional).
     * If provided, must be between 2 and 255 characters.
     */
    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    private String fullName;

    /**
     * Updated email address (optional).
     * Must be valid and unique across the system.
     */
    @Email(message = "Email must be valid")
    private String email;

    /**
     * URL to the updated avatar image (optional).
     */
    @Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
    private String avatarUrl;

    /**
     * Updated timezone preference (optional).
     * IANA timezone identifier (e.g., "America/New_York").
     */
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    /**
     * Updated currency preference (optional).
     * ISO 4217 currency code (e.g., "USD", "EUR").
     */
    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;
}

