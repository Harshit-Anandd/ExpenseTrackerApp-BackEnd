package com.spendsmart.auth.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthNotificationEvent {

    private String eventType;
    private Long userId;
    private String email;
    private String title;
    private String message;
    private String severity;
    private String otpCode;
    private Instant occurredAt;
}

