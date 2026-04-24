package com.spendsmart.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerificationDto {

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	private String email;

	@NotBlank(message = "OTP code is required")
	private String otpCode;

	@NotBlank(message = "OTP purpose is required")
	private String purpose;

	@NotBlank(message = "OTP challenge ID is required")
	private String challengeId;
}


