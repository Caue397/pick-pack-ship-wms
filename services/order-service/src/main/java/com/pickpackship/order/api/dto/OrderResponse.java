package com.pickpackship.order.api.dto;

import com.pickpackship.order.domain.Party;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID workspaceId,
        String customerName,
        UUID seller,
        Party sender,
        Party recipient,
        Map<String, Integer> items
) {
}
