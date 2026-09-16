package com.pickpackship.order.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String operatorId, UUID workspaceId, String role) {
    public static AuthenticatedUser from(Jwt jwt) {
       return new AuthenticatedUser(
               UUID.fromString(jwt.getSubject()),
               jwt.getClaimAsString("operatorId"),
               UUID.fromString(jwt.getClaimAsString("workspaceId")),
               jwt.getClaimAsString("role")
       );
    }
}
