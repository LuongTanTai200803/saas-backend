package com.saasai.exception;

public class OpenRouterEmptyResponseException extends OpenRouterException {

    public OpenRouterEmptyResponseException(String message) {
        super(
                502,
                "OPENROUTER_EMPTY_RESPONSE",
                message,
                true
        );
    }
}