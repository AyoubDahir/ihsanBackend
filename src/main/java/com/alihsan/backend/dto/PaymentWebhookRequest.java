package com.alihsan.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentWebhookRequest(
    @NotBlank String referenceId,
    @NotBlank String status,
    String providerTxnId
) {}
