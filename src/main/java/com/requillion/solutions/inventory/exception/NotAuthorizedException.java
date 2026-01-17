package com.requillion.solutions.inventory.exception;

public class NotAuthorizedException extends ApiException {

    public NotAuthorizedException(String userMessage, String systemMessageFormat, Object... args) {
        super(userMessage, systemMessageFormat, args);
    }
}
