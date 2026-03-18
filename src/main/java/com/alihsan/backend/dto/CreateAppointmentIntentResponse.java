package com.alihsan.backend.dto;

public record CreateAppointmentIntentResponse(
    String referenceId,
    String status,
    String message
) {}
