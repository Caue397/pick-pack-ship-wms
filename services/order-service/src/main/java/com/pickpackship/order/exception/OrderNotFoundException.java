package com.pickpackship.order.exception;

public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException() {
        super("Order not found");
    }
}
