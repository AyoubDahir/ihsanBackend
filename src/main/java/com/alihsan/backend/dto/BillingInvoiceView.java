package com.alihsan.backend.dto;

import java.util.List;
import java.util.Map;

public record BillingInvoiceView(
    String invoiceId,
    String postingDate,
    String dueDate,
    String status,
    String currency,
    String grandTotal,
    String outstandingAmount,
    List<Map<String, Object>> items
) {}
