package com.pickpackship.auth.service;

import com.pickpackship.auth.api.dto.CreateUserRequest;
import com.pickpackship.auth.api.dto.LoginRequest;
import com.pickpackship.auth.api.dto.SignUpRequest;
import com.pickpackship.auth.domain.Role;
import com.pickpackship.auth.domain.User;
import com.pickpackship.auth.domain.Workspace;
import com.pickpackship.auth.exception.DuplicateUserNameException;
import com.pickpackship.auth.exception.InvalidCredentialsException;
import com.pickpackship.auth.outbox.OutboxWriter;
import com.pickpackship.auth.outbox.WorkspaceDeletedPayload;
import com.pickpackship.auth.repository.UserRepository;
import com.pickpackship.auth.repository.WorkspaceRepository;
import com.pickpackship.auth.security.IssuedToken;
import com.pickpackship.auth.security.JwtService;
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
    private final UserService userService;

    @Transactional
    public IssuedToken signUp(SignUpRequest request) {
        if (userRepository.findByUserName(request.userName()).isPresent()) {
            throw new DuplicateUserNameException();
        }

        Workspace workspace = Workspace.build(request.workspaceName());
        workspaceRepository.save(workspace);

        CreateUserRequest payload = new CreateUserRequest(
                request.userName(),
                request.password(),
                Role.ADMIN
        );

        User user = userService.createUser(payload, workspace.getWorkspaceId());

        return jwtService.issueToken(user);
    }

    public IssuedToken logIn(LoginRequest request) {
        User user = userRepository.findByUserName(request.userName())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return jwtService.issueToken(user);
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
}
