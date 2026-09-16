package com.pickpackship.order.exception;

public class SellerNotExistsException extends DomainException {
    public SellerNotExistsException() {
        super("Seller not exists");
    }
}
