package com.pickpackship.auth.service;

import com.pickpackship.auth.api.dto.CreateUserRequest;
import com.pickpackship.auth.api.dto.LoginRequest;
import com.pickpackship.auth.api.dto.SignUpRequest;
import com.pickpackship.auth.api.dto.UserResponse;
import com.pickpackship.auth.domain.Role;
import com.pickpackship.auth.domain.User;
import com.pickpackship.auth.domain.Workspace;
import com.pickpackship.auth.exception.DuplicateUserNameException;
import com.pickpackship.auth.exception.InvalidCredentialsException;
import com.pickpackship.auth.exception.UserNotFoundException;
import com.pickpackship.auth.outbox.OutboxWriter;
import com.pickpackship.auth.outbox.WorkspaceDeletedPayload;
import com.pickpackship.auth.repository.UserRepository;
import com.pickpackship.auth.repository.WorkspaceRepository;
import com.pickpackship.auth.security.IssuedToken;
import com.pickpackship.auth.security.JwtService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OutboxWriter outboxWriter;

    @Transactional
    public IssuedToken signUp(SignUpRequest request) {
        if (userRepository.findByUserName(request.userName()).isPresent()) {
            throw new DuplicateUserNameException();
        }

        Workspace workspace = Workspace.build(request.workspaceName());
        workspaceRepository.save(workspace);

        User admin = User.build(
                workspace.getWorkspaceId(),
                request.userName(),
                passwordEncoder.encode(request.password()),
                UUID.randomUUID().toString(),
                Role.ADMIN
        );
        userRepository.save(admin);

        return jwtService.issueToken(admin);
    }

    public IssuedToken logIn(LoginRequest request) {
        User user = userRepository.findByUserName(request.userName())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return jwtService.issueToken(user);
    }

    public UserResponse createUser(CreateUserRequest request, Jwt caller) {
        if (userRepository.findByUserName(request.userName()).isPresent()) {
            throw new DuplicateUserNameException();
        }

        UUID workspaceId = UUID.fromString(caller.getClaimAsString("workspaceId"));

        User user = User.build(
                workspaceId,
                request.userName(),
                passwordEncoder.encode(request.password()),
                request.operatorId(),
                request.role()
        );
        userRepository.save(user);

        return toResponse(user);
    }

    public List<UserResponse> listUsers(Jwt caller) {
        UUID workspaceId = UUID.fromString(caller.getClaimAsString("workspaceId"));

        return userRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUser(UUID userId, Jwt caller) {
        UUID callerId = UUID.fromString(caller.getSubject());
        UUID callerWorkspaceId = UUID.fromString(caller.getClaimAsString("workspaceId"));
        boolean isAdmin = Role.ADMIN.name().equals(caller.getClaimAsString("role"));

        if (!isAdmin && !userId.equals(callerId)) {
            throw new AccessDeniedException("You can only view your own account");
        }

        User user = userRepository.findById(userId)
                .filter(u -> u.getWorkspaceId().equals(callerWorkspaceId))
                .orElseThrow(UserNotFoundException::new);

        return toResponse(user);
    }

    @Transactional
    public void deleteWorkspace(UUID workspaceId, Jwt caller) {
        UUID callerWorkspaceId = UUID.fromString(caller.getClaimAsString("workspaceId"));
        if (!callerWorkspaceId.equals(workspaceId)) {
            throw new AccessDeniedException("You can only delete your own workspace");
        }

        userRepository.deleteByWorkspaceId(workspaceId);
        workspaceRepository.deleteById(workspaceId);

        outboxWriter.write("workspace.deleted", new WorkspaceDeletedPayload(workspaceId));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getUserId(), user.getUserName(), user.getOperatorId(), user.getRole());
    }
}
