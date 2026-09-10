package com.pickpackship.auth;

import com.pickpackship.auth.api.dto.SignUpRequest;
import com.pickpackship.auth.domain.User;
import com.pickpackship.auth.domain.Workspace;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

class SignUpIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void signUpCreatesWorkspaceAndAdminUserAndReturnsValidJwt() {
        SignUpRequest request = new SignUpRequest("Acme Logistics", "admin.acme", "supersecret123");

        ResponseEntity<Void> response = restTemplate.postForEntity("/auth/signup", request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String tokenCookie = extractAccessTokenCookie(response);
        String tokenValue = tokenCookie.substring(tokenCookie.indexOf('=') + 1);
        Jwt jwt = jwtDecoder.decode(tokenValue);

        Optional<User> savedUser = userRepository.findByUserName("admin.acme");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getRole().name()).isEqualTo("ADMIN");

        Optional<Workspace> savedWorkspace = workspaceRepository.findById(savedUser.get().getWorkspaceId());
        assertThat(savedWorkspace).isPresent();
        assertThat(savedWorkspace.get().getName()).isEqualTo("Acme Logistics");

        assertThat(jwt.getSubject()).isEqualTo(savedUser.get().getUserId().toString());
        assertThat(jwt.getClaimAsString("workspaceId")).isEqualTo(savedWorkspace.get().getWorkspaceId().toString());
        assertThat(jwt.getClaimAsString("operatorId")).isEqualTo(savedUser.get().getOperatorId());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
    }
}
