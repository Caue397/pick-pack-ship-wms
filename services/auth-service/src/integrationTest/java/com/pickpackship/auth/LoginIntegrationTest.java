package com.pickpackship.auth;

import com.pickpackship.auth.api.dto.LoginRequest;
import com.pickpackship.auth.api.dto.SignUpRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class LoginIntegrationTest extends AbstractIntegrationTest {

    @Test
    void loginWithWrongPasswordReturnsUnauthorizedWithGenericMessage() {
        SignUpRequest signUp = new SignUpRequest("Acme Logistics", "admin.acme", "correctPassword1");
        restTemplate.postForEntity("/auth/signup", signUp, Void.class);

        LoginRequest wrongPassword = new LoginRequest("admin.acme", "wrongPassword1");
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/login", wrongPassword, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("Invalid credentials");
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
    }
}
