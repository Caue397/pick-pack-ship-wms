package com.pickpackship.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "sellers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sellers_workspace_document",
                columnNames = {"workspace_id", "document"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller {

    public static Seller build(
            UUID workspaceId,
            String name,
            String document
    ) {
        Seller seller = new Seller();
        seller.workspaceId = workspaceId;
        seller.name = name;
        seller.document = document;
        return seller;
    }

    public void rename(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sellerId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String document;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
