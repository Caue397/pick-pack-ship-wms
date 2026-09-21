package com.pickpackship.order.api.dto;

import com.pickpackship.order.domain.OrderStatus;

import java.util.UUID;

public record OrderFilter(
        Long orderNumber,
        String customerName,
        UUID seller,
        OrderStatus status
) {
}
