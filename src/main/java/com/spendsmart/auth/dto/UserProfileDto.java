/**
 * UserProfileDto is the data transfer object for user profile responses.
 *
 * <p>Returned by the GET /auth/profile endpoint. Contains all user information
 * except the password, which is never exposed to the client.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {

    /**
     * Unique user identifier.
     */
    private Long userId;

    /**
     * Full name of the user.
     */
    private String fullName;

    /**
     * Email address of the user.
     */
    private String email;

    /**
     * URL to the user's avatar image.
     */
    private String avatarUrl;

    /**
     * User's preferred currency (ISO 4217 code).
     */
    private String currency;

    /**
     * User's timezone (IANA identifier).
     */
    private String timezone;

    /**
     * Authentication provider used (LOCAL or GOOGLE).
     */
    private String provider;

    /**
     * User's role in the system.
     */
    private String role;

    /**
     * Account activation status.
     */
    private Boolean isActive;

    /**
     * User's monthly budget.
     */
    private Double monthlyBudget;

    /**
     * Account creation timestamp (UTC).
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Account last update timestamp (UTC).
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}

