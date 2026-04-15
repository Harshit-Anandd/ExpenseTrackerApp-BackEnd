/**
 * DeactivatedAccountException is thrown when a deactivated user attempts to log in.
 * These users have soft-deleted accounts (isActive = false) and cannot authenticate.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.exception;

public class DeactivatedAccountException extends RuntimeException {

    public DeactivatedAccountException(String message) {
        super(message);
    }

    public DeactivatedAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}

