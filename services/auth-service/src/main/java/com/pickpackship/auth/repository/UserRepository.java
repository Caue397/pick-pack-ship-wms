package com.pickpackship.auth.repository;

import com.pickpackship.auth.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUserName(String userName);

    List<User> findByWorkspaceId(UUID workspaceId);

    void deleteByWorkspaceId(UUID workspaceId);
}
