package com.spendsmart.auth.service;

public interface OtpChallengeService {

    OtpChallenge createChallenge(String email, OtpPurpose purpose);

    boolean verify(String email, OtpPurpose purpose, String challengeId, String otpCode);
}

