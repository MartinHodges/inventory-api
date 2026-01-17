package com.requillion.solutions.inventory.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Data
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class ErrorResponse {
        private String error;
        private String message;
        private Integer status;
    }

    private void logError(String message, HttpStatus status) {
        LoggerUtil.error(log, "%s [%d]", message, status.value());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        logError(ex.getFullMessage(), status);
        return ResponseEntity.status(status)
                .body(new ErrorResponse("NOT_FOUND", ex.getUserMessage(), status.value()));
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<ErrorResponse> handleNotAuthorized(NotAuthorizedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        logError(ex.getFullMessage(), status);
        return ResponseEntity.status(status)
                .body(new ErrorResponse("FORBIDDEN", ex.getUserMessage(), status.value()));
    }

    @ExceptionHandler(NotAuthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleNotAuthenticated(NotAuthenticatedException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        logError(ex.getFullMessage(), status);
        return ResponseEntity.status(status)
                .body(new ErrorResponse("UNAUTHORIZED", ex.getUserMessage(), status.value()));
    }

    @ExceptionHandler(BadInputException.class)
    public ResponseEntity<ErrorResponse> handleBadInput(BadInputException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logError(ex.getFullMessage(), status);
        return ResponseEntity.status(status)
                .body(new ErrorResponse("BAD_REQUEST", ex.getUserMessage(), status.value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        LoggerUtil.error(log, "Validation error: %s", errors);
        return ResponseEntity.status(status)
                .body(new ErrorResponse("VALIDATION_ERROR", errors, status.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalError(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        LoggerUtil.error(log, "Internal server error", ex);
        return ResponseEntity.status(status)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR",
                        "We had a problem doing what you asked. [" + ex.getMessage() + "]",
                        status.value()));
    }
}
