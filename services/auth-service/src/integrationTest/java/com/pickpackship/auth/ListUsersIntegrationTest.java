package com.pickpackship.auth;

import com.pickpackship.auth.api.dto.CreateUserRequest;
import com.pickpackship.auth.api.dto.LoginRequest;
import com.pickpackship.auth.api.dto.SignUpRequest;
import com.pickpackship.auth.api.dto.UserResponse;
import com.pickpackship.auth.domain.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ListUsersIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminListsOnlyUsersFromOwnWorkspace() {
        String adminCookie = signUpAndExtractCookie("Acme Logistics", "admin.acme", "adminPassword1");
        createUser(adminCookie, "picker.acme", "pickerPassword1", "OP-001", Role.PICKER);

        // Second workspace: must not leak into the first workspace's listing.
        signUpAndExtractCookie("Globex", "admin.globex", "adminPassword1");

        HttpEntity<Void> listRequest = authenticatedRequest(adminCookie);
        ResponseEntity<List<UserResponse>> response = restTemplate.exchange(
                "/auth/users", HttpMethod.GET, listRequest, new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(UserResponse::userName, UserResponse::role)
                .containsExactlyInAnyOrder(
                        tuple("admin.acme", Role.ADMIN),
                        tuple("picker.acme", Role.PICKER));
    }

    @Test
    void nonAdminCannotListUsers() {
        String adminCookie = signUpAndExtractCookie("Acme Logistics", "admin.acme", "adminPassword1");
        createUser(adminCookie, "picker.acme", "pickerPassword1", "OP-001", Role.PICKER);

        LoginRequest pickerLogin = new LoginRequest("picker.acme", "pickerPassword1");
        ResponseEntity<Void> pickerLoginResponse = restTemplate.postForEntity("/auth/login", pickerLogin, Void.class);
        String pickerCookie = extractAccessTokenCookie(pickerLoginResponse);

        HttpEntity<Void> listRequest = authenticatedRequest(pickerCookie);
        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/users", HttpMethod.GET, listRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
