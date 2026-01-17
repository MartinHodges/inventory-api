package com.requillion.solutions.inventory.exception;

import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {

    private final String userMessage;
    private final String systemMessage;

    public ApiException(String userMessage, String systemMessageFormat, Object... args) {
        super(userMessage + " | " + String.format(systemMessageFormat, args));
        this.userMessage = userMessage;
        this.systemMessage = String.format(systemMessageFormat, args);
    }

    public String getFullMessage() {
        return userMessage + " | " + systemMessage;
    }
}
