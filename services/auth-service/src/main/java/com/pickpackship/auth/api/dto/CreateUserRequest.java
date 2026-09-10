package com.pickpackship.auth.api.dto;

import com.pickpackship.auth.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "userName is required")
        String userName,

        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,

        @NotBlank(message = "operatorId is required")
        String operatorId,

        @NotNull(message = "role is required")
        Role role
) {}
