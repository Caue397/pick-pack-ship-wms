package com.pickpackship.order.repository;

import com.pickpackship.order.domain.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerRepository extends JpaRepository<Seller, UUID> {
    Optional<Seller> findByDocument(String document);
    List<Seller> findByWorkspaceId(UUID workspaceId);
}
