package com.requillion.solutions.inventory.exception;

public class BadInputException extends ApiException {

    public BadInputException(String userMessage, String systemMessageFormat, Object... args) {
        super(userMessage, systemMessageFormat, args);
    }
}
