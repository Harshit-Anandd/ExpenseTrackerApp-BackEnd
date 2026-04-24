package com.spendsmart.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDto {

    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private String subscriptionType;
    private Boolean isActive;
    private String provider;
    private Boolean emailVerified;
    private Boolean twoFactorEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}

