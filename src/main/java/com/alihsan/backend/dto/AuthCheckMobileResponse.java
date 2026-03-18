package com.alihsan.backend.dto;

public record AuthCheckMobileResponse(
    boolean exists,
    String mobile,
    String patientId,
    String patientName,
    String nextStep
) {
}
