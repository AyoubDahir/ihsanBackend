package com.alihsan.backend.dto;

public record BillingInvoiceView(
    String invoiceId,
    String postingDate,
    String dueDate,
    String status,
    String currency,
    String grandTotal,
    String outstandingAmount
) {}
