package com.alihsan.backend.dto;

public record AuthSendOtpResponse(
    boolean sent,
    String mobile,
    String expiresAt,
    String nextStep,
    String otp
) {
}
