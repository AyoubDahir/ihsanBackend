package com.alihsan.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAppointmentIntentRequest(
    String patientId,
    String firstName,
    String lastName,
    String fullName,
    String mobile,
    String sex,
    @NotBlank String practitionerId,
    @NotBlank String appointmentDate,
    @NotBlank String appointmentTime,
    @NotBlank String department,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank String phoneNumber,
    @NotBlank String paymentType
) {}
