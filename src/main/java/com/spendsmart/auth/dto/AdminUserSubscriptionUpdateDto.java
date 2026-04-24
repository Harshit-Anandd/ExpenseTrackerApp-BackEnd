package com.spendsmart.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserSubscriptionUpdateDto {

    @NotBlank(message = "subscriptionType is required")
    private String subscriptionType;
}

