package com.alihsan.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateInvoicePaymentRequest(
    @NotBlank String patientId,
    @NotBlank String invoiceId,
    @DecimalMin("0.01") BigDecimal amount,
    @NotBlank String phoneNumber,
    @NotBlank String paymentType
) {}
