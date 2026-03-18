package com.alihsan.backend.dto;

public record PractitionerSlotView(
    String slotName,
    String serviceUnit,
    String date,
    String fromTime,
    String toTime,
    boolean available,
    int bookedCount,
    Integer capacity,
    Integer remainingCapacity,
    boolean teleConf,
    boolean allowOverlap
) {
}
