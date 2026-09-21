package com.pickpackship.auth;

import com.pickpackship.auth.api.dto.CreateUserRequest;
import com.pickpackship.auth.api.dto.LoginRequest;
import com.pickpackship.auth.api.dto.SignUpRequest;
import com.pickpackship.auth.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CreateUserAuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createUserCalledByNonAdminIsForbidden() {
        SignUpRequest signUp = new SignUpRequest("Acme Logistics", "admin.acme", "adminPassword1");
        ResponseEntity<Void> signUpResponse = restTemplate.postForEntity("/auth/signup", signUp, Void.class);
        String adminCookie = extractAccessTokenCookie(signUpResponse);

        CreateUserRequest createPicker = new CreateUserRequest("picker.acme", "pickerPassword1", Role.PICKER);
        HttpEntity<CreateUserRequest> createPickerRequest = authenticatedJsonRequest(adminCookie, createPicker);
        ResponseEntity<Void> createPickerResponse =
                restTemplate.postForEntity("/auth/user", createPickerRequest, Void.class);
        assertThat(createPickerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LoginRequest pickerLogin = new LoginRequest("picker.acme", "pickerPassword1");
        ResponseEntity<Void> pickerLoginResponse = restTemplate.postForEntity("/auth/login", pickerLogin, Void.class);
        String pickerCookie = extractAccessTokenCookie(pickerLoginResponse);

        CreateUserRequest anotherUser = new CreateUserRequest("checker.acme", "checkerPassword1", Role.CHECKER);
        HttpEntity<CreateUserRequest> forbiddenRequest = authenticatedJsonRequest(pickerCookie, anotherUser);
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/user", forbiddenRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
