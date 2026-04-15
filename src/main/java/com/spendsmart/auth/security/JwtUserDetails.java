/**
 * JwtUserDetails is a simple class used to store additional user information
 * in the authentication context. This allows downstream services to access
 * user identity without making additional database calls.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtUserDetails {
    private Long userId;
    private String email;
    private String role;
}

