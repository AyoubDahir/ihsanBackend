package com.alihsan.backend.dto;

public record AuthVerifyOtpResponse(
    boolean authenticated,
    String mobile,
    boolean patientExists,
    String patientId,
    String patientName,
    String nextStep
) {
}
