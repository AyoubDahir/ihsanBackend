package com.alihsan.backend.dto;

public record PractitionerView(
    String id,
    String name,
    String department,
    String consultationPrice,
    boolean active
) {}
