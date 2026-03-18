package com.alihsan.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthCheckMobileRequest(
    @NotBlank String mobile
) {
}
