package com.pickpackship.order.api.dto;

public record CancelOrderRequest(
        String cancellationReason
) {
}
