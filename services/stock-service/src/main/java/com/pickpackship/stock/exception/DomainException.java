package com.pickpackship.stock.exception;

public class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
