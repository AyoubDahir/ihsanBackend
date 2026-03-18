package com.alihsan.backend.dto;

public record LabReportView(
    String name,
    String labTestName,
    String status,
    String resultDate,
    String downloadUrl
) {}
