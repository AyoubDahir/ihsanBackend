package com.alihsan.backend.dto;

public record AuthSelfRegisterResponse(
    boolean created,
    String patientId,
    String patientName,
    String mobile,
    String nextStep
) {
}
