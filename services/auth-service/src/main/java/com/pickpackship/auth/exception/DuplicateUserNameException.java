package com.pickpackship.auth.exception;

public class DuplicateUserNameException extends DomainException {
    public DuplicateUserNameException() {
        super("User already exists");
    }
}
