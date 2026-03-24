package com.alihsan.backend.dto;

public record PrescriptionDrugView(
    String drugCode,
    String drugName,
    String qty,
    String dosage,
    String period,
    String route,
    String instructions
) {}
