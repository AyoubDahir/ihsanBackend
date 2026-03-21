package com.alihsan.backend.dto;

public record QueueStatusView(
    boolean found,
    String que,
    Integer tokenNo,
    String patientName,
    String practitionerName,
    String department,
    String status,
    String queSteps,
    Integer patientsAhead
) {}
