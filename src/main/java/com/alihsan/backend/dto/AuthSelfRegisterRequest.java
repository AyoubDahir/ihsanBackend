package com.alihsan.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthSelfRegisterRequest(
    String firstName,
    String lastName,
    String fullName,
    @NotBlank String mobile,
    String sex,
    @NotNull Integer age,
    @NotBlank String ageType
) {
}
