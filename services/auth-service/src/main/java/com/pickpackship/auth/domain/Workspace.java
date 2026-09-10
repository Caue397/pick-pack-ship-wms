package com.pickpackship.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "workspaces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace {

    public static Workspace build(
            String name
    ) {
        Workspace workspace = new Workspace();
        workspace.name = name;
        return workspace;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID workspaceId;

    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
