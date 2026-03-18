package com.alihsan.backend.dto;

public record CreateInvoicePaymentResponse(
    String referenceId,
    String invoiceId,
    String status,
    String message
) {}
