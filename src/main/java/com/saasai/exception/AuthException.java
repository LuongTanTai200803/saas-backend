package com.saasai.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {
    private final HttpStatus status;

    public AuthException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status == null ? HttpStatus.BAD_REQUEST : status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}