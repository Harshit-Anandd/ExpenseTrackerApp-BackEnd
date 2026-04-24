package com.spendsmart.auth.security;

public interface TokenBlocklistService {

    void revokeToken(String token, long ttlMillis);

    boolean isRevoked(String token);
}

