package com.alihsan.backend.dto;

import java.util.List;

public record PrescriptionView(
    String encounter,
    String date,
    String practitioner,
    String practitionerName,
    List<PrescriptionDrugView> drugs
) {}
