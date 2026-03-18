package com.alihsan.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthVerifyOtpRequest(
    @NotBlank String mobile,
    @NotBlank String otp
) {
}
