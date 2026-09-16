package com.pickpackship.order.exception;

public class DuplicateSellerDocumentException extends DomainException {
    public DuplicateSellerDocumentException() {
        super("Already exists a seller registered with this document");
    }
}
