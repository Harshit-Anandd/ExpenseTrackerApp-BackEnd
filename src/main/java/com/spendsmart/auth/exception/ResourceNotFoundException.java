/**
 * ResourceNotFoundException is thrown when a requested resource (e.g., user) is not found
 * in the database. Used for getUserById, getUserByEmail, etc.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

