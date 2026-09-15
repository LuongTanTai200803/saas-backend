package com.saasai.exception;

public class OpenRouterTimeoutException extends OpenRouterException {

    public OpenRouterTimeoutException(String message, Throwable cause) {
        super(
                408,
                "OPENROUTER_TIMEOUT",
                message,
                true
        );

        initCause(cause);
    }
}