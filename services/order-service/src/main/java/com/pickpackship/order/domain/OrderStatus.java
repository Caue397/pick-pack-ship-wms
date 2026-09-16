package com.pickpackship.order.domain;

public enum OrderStatus {
    CREATED,
    STOCK_RESERVED,
    PICKING,
    PICKED,
    CHECKING,
    AWAITING_REPICK,
    CHECKED,
    SHIPPED,
    AWAITING_PICKUP,
    CANCELLED,
    DIVERGENT
}
