package com.pickpackship.order.outbox;

import java.util.Map;
import java.util.UUID;

public record OrderCancelledPayload(UUID orderId, UUID workspaceId, Map<String, Integer> items) {
}
