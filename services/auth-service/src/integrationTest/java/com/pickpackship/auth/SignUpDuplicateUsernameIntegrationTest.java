package com.pickpackship.auth;

import com.pickpackship.auth.api.dto.SignUpRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SignUpDuplicateUsernameIntegrationTest extends AbstractIntegrationTest {

    @Test
    void signUpWithExistingUsernameReturnsConflictAndCreatesNothing() {
        SignUpRequest first = new SignUpRequest("Acme Logistics", "admin.acme", "supersecret123");
        restTemplate.postForEntity("/auth/signup", first, Void.class);

        long workspacesAfterFirstSignUp = workspaceRepository.count();
        long usersAfterFirstSignUp = userRepository.count();

        SignUpRequest duplicate = new SignUpRequest("Other Company", "admin.acme", "anotherPassword1");
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/signup", duplicate, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(workspaceRepository.count()).isEqualTo(workspacesAfterFirstSignUp);
        assertThat(userRepository.count()).isEqualTo(usersAfterFirstSignUp);
    }
}
