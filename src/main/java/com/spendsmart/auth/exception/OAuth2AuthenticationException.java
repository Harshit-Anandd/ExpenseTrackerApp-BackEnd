/**
 * OAuth2AuthenticationException is thrown when OAuth2 authentication (e.g., Google login)
 * fails due to invalid credentials or other authentication errors.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.exception;

public class OAuth2AuthenticationException extends RuntimeException {

    public OAuth2AuthenticationException(String message) {
        super(message);
    }

    public OAuth2AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

