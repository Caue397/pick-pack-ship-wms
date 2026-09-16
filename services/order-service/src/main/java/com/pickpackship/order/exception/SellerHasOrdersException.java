package com.pickpackship.order.exception;

public class SellerHasOrdersException extends DomainException {
    public SellerHasOrdersException() {
        super("This seller has registered orders");
    }
}
