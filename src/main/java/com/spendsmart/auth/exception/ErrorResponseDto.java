/**
 * ErrorResponseDto is the standardized error response structure returned by the API.
 *
 * <p>All exceptions are mapped to this consistent JSON format, ensuring a uniform
 * error handling experience across the frontend.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponseDto {

    /**
     * HTTP status code of the error response.
     */
    private int status;

    /**
     * Short error type identifier (e.g., "INVALID_CREDENTIALS", "USER_ALREADY_EXISTS").
     */
    private String error;

    /**
     * Human-readable error message suitable for displaying to users.
     */
    private String message;

    /**
     * Request path that triggered the error.
     */
    private String path;

    /**
     * Timestamp when the error occurred (UTC).
     */
    @Builder.Default
    private String timestamp = java.time.LocalDateTime.now().toString();

    /**
     * Additional details about the error (optional).
     * Used for validation errors or other structured error information.
     */
    private Object details;
}

