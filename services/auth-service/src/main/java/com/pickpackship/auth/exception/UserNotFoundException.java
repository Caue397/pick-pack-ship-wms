package com.pickpackship.auth.exception;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException() {
        super("User not found");
    }
}
