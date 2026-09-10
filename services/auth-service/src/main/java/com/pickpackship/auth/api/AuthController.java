package com.pickpackship.auth.api;

import com.pickpackship.auth.api.dto.CreateUserRequest;
import com.pickpackship.auth.api.dto.LoginRequest;
import com.pickpackship.auth.api.dto.SignUpRequest;
import com.pickpackship.auth.api.dto.UserResponse;
import com.pickpackship.auth.security.CookieBearerTokenResolver;
import com.pickpackship.auth.security.IssuedToken;
import com.pickpackship.auth.service.AuthService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.cookie-secure:true}")
    private boolean cookieSecure;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest request) {
        IssuedToken issued = authService.signUp(request);
        ResponseCookie cookie = authCookie(issued.token(), Duration.between(Instant.now(), issued.expiresAt()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> logIn(@Valid @RequestBody LoginRequest request) {
        IssuedToken issued = authService.logIn(request);
        ResponseCookie cookie = authCookie(issued.token(), Duration.between(Instant.now(), issued.expiresAt()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logOut() {
        ResponseCookie cookie = authCookie("", Duration.ZERO);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal Jwt caller
    ) {
        UserResponse response = authService.createUser(request, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers(@AuthenticationPrincipal Jwt caller) {
        return ResponseEntity.ok(authService.listUsers(caller));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt caller
    ) {
        return ResponseEntity.ok(authService.getUser(userId, caller));
    }

    @DeleteMapping("/workspaces/{workspaceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt caller
    ) {
        authService.deleteWorkspace(workspaceId, caller);
        ResponseCookie cookie = authCookie("", Duration.ZERO);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseCookie authCookie(String value, Duration maxAge) {
        return ResponseCookie.from(CookieBearerTokenResolver.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
