package com.pickpackship.order.api.dto;

import java.util.UUID;

public record SellerResponse(
        UUID sellerId,
        UUID workspaceId,
        String name,
        String document
) {}
