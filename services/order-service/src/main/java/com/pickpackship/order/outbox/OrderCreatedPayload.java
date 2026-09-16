package com.pickpackship.order.outbox;

import java.util.Map;
import java.util.UUID;

public record OrderCreatedPayload(UUID orderId, UUID workspaceId, Map<String, Integer> items) {}
