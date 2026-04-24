package com.spendsmart.auth.service;

public enum OtpPurpose {
    SIGNUP,
    LOGIN_2FA;

    public static OtpPurpose from(String value) {
        return OtpPurpose.valueOf(value.trim().toUpperCase());
    }
}

