package com.pickpackship.auth.api.dto;

import com.pickpackship.auth.domain.Role;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String userName,
        UUID operatorId,
        Role role
) {}
