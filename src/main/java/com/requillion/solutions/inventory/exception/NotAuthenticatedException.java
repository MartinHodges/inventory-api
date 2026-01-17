package com.requillion.solutions.inventory.exception;

public class NotAuthenticatedException extends ApiException {

    public NotAuthenticatedException(String userMessage, String systemMessageFormat, Object... args) {
        super(userMessage, systemMessageFormat, args);
    }
}
