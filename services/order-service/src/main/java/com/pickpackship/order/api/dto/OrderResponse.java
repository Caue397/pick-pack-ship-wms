package com.pickpackship.order.api.dto;

import com.pickpackship.order.domain.OrderStatus;
import com.pickpackship.order.domain.Party;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID workspaceId,
        String customerName,
        Long orderNumber,
        UUID seller,
        Party sender,
        Party recipient,
        Map<String, Integer> items,
        OrderStatus status,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {
}
