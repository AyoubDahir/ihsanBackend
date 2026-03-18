package com.alihsan.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthSendOtpRequest(
    @NotBlank String mobile
) {
}
