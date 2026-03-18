package com.alihsan.backend.dto;

public record PatientAppointmentView(
    String name,
    String patient,
    String practitioner,
    String practitionerName,
    String appointmentDate,
    String appointmentTime,
    String status,
    String department
) {}
