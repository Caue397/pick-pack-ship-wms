package com.pickpackship.auth.api;

import com.pickpackship.auth.api.dto.CreateUserRequest;
import com.pickpackship.auth.api.dto.UserResponse;
import com.pickpackship.auth.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createWorkspaceMember(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal Jwt caller
    ) {
        UserResponse response = userService.createWorkspaceMember(request, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers(@AuthenticationPrincipal Jwt caller) {
        return ResponseEntity.ok(userService.listUsers(caller));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt caller
    ) {
        return ResponseEntity.ok(userService.getUser(userId, caller));
    }
}
