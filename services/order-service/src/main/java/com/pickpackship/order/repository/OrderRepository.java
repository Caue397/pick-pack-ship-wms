package com.pickpackship.order.repository;

import com.pickpackship.order.api.dto.SummaryOrderResponse;
import com.pickpackship.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    boolean existsBySeller(UUID seller);
    List<Order> findByWorkspaceId(UUID workspaceId);

    @Query("""
            SELECT new com.pickpackship.order.api.dto.SummaryOrderResponse(
                o.orderId, o.workspaceId, o.customerName, o.orderNumber,
                s.name, o.status, SIZE(o.items)
            )
            FROM Order o
            JOIN Seller s ON o.seller = s.sellerId
            WHERE o.workspaceId = :workspaceId
            """)
    List<SummaryOrderResponse> findSummariesByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
