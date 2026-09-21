package com.pickpackship.order.repository;

import com.pickpackship.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    boolean existsBySeller(UUID seller);
    Optional<Order> findByOrderIdAndWorkspaceId(UUID orderId, UUID workspaceId);
}
