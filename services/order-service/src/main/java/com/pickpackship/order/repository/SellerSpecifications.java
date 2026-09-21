package com.pickpackship.order.repository;

import com.pickpackship.order.domain.Seller;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class SellerSpecifications {

    public static Specification<Seller> hasWorkspaceId(UUID workspaceId) {
        return (root, query, cb) -> cb.equal(root.get("workspaceId"), workspaceId);
    }

    public static Specification<Seller> hasName(String name) {
        return name == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Seller> hasDocument(String document) {
        return document == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("document"), document);
    }
}
