package com.requillion.solutions.inventory.exception;

public class NotFoundException extends ApiException {

    public NotFoundException(String userMessage, String systemMessageFormat, Object... args) {
        super(userMessage, systemMessageFormat, args);
    }
}
