package com.pickpackship.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "userName is required")
        String userName,

        @NotBlank(message = "password is required")
        String password
) {}
