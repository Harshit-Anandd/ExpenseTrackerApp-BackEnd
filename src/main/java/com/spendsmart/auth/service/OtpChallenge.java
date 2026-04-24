package com.spendsmart.auth.service;

public record OtpChallenge(String challengeId, long expiresInSeconds, String otpCode) {
}

