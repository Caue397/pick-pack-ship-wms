package com.pickpackship.auth;

import com.pickpackship.auth.api.dto.CreateUserRequest;
import com.pickpackship.auth.api.dto.LoginRequest;
import com.pickpackship.auth.api.dto.SignUpRequest;
import com.pickpackship.auth.api.dto.UserResponse;
import com.pickpackship.auth.domain.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GetUserIntegrationTest extends AbstractIntegrationTest {

    @Test
    void userCanViewOwnAccount() {
        String adminCookie = signUpAndExtractCookie("Acme Logistics", "admin.acme", "adminPassword1");
        UUID adminId = userRepository.findByUserName("admin.acme").orElseThrow().getUserId();

        ResponseEntity<UserResponse> response = getUser(adminCookie, adminId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().userName()).isEqualTo("admin.acme");
    }

    @Test
    void nonAdminCannotViewAnotherUser() {
        String adminCookie = signUpAndExtractCookie("Acme Logistics", "admin.acme", "adminPassword1");
        createUser(adminCookie, "picker.acme", "pickerPassword1", "OP-001", Role.PICKER);
        UUID adminId = userRepository.findByUserName("admin.acme").orElseThrow().getUserId();

        LoginRequest pickerLogin = new LoginRequest("picker.acme", "pickerPassword1");
        ResponseEntity<Void> pickerLoginResponse = restTemplate.postForEntity("/auth/login", pickerLogin, Void.class);
        String pickerCookie = extractAccessTokenCookie(pickerLoginResponse);

        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/users/" + adminId, HttpMethod.GET, authenticatedRequest(pickerCookie), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanViewAnotherUserFromSameWorkspace() {
        String adminCookie = signUpAndExtractCookie("Acme Logistics", "admin.acme", "adminPassword1");
        createUser(adminCookie, "picker.acme", "pickerPassword1", "OP-001", Role.PICKER);
        UUID pickerId = userRepository.findByUserName("picker.acme").orElseThrow().getUserId();

        ResponseEntity<UserResponse> response = getUser(adminCookie, pickerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().userName()).isEqualTo("picker.acme");
    }

    @Test
    void adminCannotViewUserFromAnotherWorkspace() {
        String adminAcmeCookie = signUpAndExtractCookie("Acme Logistics", "admin.acme", "adminPassword1");
        signUpAndExtractCookie("Globex", "admin.globex", "adminPassword1");
        UUID globexAdminId = userRepository.findByUserName("admin.globex").orElseThrow().getUserId();

        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/users/" + globexAdminId, HttpMethod.GET, authenticatedRequest(adminAcmeCookie), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void viewingNonExistentUserReturnsNotFound() {
        String adminCookie = signUpAndExtractCookie("Acme Logistics", "admin.acme", "adminPassword1");

        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/users/" + UUID.randomUUID(), HttpMethod.GET, authenticatedRequest(adminCookie), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<UserResponse> getUser(String cookie, UUID userId) {
        return restTemplate.exchange(
                "/auth/users/" + userId, HttpMethod.GET, authenticatedRequest(cookie), UserResponse.class);
    }

    private String signUpAndExtractCookie(String workspaceName, String userName, String password) {
        SignUpRequest signUp = new SignUpRequest(workspaceName, userName, password);
        ResponseEntity<Void> signUpResponse = restTemplate.postForEntity("/auth/signup", signUp, Void.class);
        return extractAccessTokenCookie(signUpResponse);
    }

    private void createUser(String adminCookie, String userName, String password, String operatorId, Role role) {
        CreateUserRequest createUser = new CreateUserRequest(userName, password, operatorId, role);
        HttpEntity<CreateUserRequest> request = authenticatedJsonRequest(adminCookie, createUser);
        ResponseEntity<Void> response = restTemplate.postForEntity("/auth/users", request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
