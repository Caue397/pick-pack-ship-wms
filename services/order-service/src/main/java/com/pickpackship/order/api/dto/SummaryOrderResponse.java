package com.pickpackship.order.api.dto;

import com.pickpackship.order.domain.OrderStatus;

import java.util.UUID;

public record SummaryOrderResponse(
        UUID orderId,
        UUID workspaceId,
        String customerName,
        Long orderNumber,
        String sellerName,
        OrderStatus status,
        Integer itemsLength
) {}
