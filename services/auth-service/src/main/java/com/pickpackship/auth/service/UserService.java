package com.pickpackship.auth.service;

import com.pickpackship.auth.api.dto.CreateUserRequest;
import com.pickpackship.auth.api.dto.UserResponse;
import com.pickpackship.auth.domain.Role;
import com.pickpackship.auth.domain.User;
import com.pickpackship.auth.exception.DuplicateUserNameException;
import com.pickpackship.auth.exception.UserNotFoundException;
import com.pickpackship.auth.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(CreateUserRequest userToCreate, UUID workspaceId) {
        User user = User.build(
                workspaceId,
                userToCreate.userName(),
                passwordEncoder.encode(userToCreate.password()),
                UUID.randomUUID(),
                userToCreate.role()
        );

        userRepository.save(user);

        return user;
    }

    public UserResponse createWorkspaceMember(CreateUserRequest request, Jwt caller) {
        if (userRepository.findByUserName(request.userName()).isPresent()) {
            throw new DuplicateUserNameException();
        }

        UUID workspaceId = UUID.fromString(caller.getClaimAsString("workspaceId"));
        User user = createUser(request, workspaceId);

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

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getUserId(), user.getUserName(), user.getOperatorId(), user.getRole());
    }
}
