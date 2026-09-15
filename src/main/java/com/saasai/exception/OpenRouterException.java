package com.saasai.exception;

public class OpenRouterException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;
    private final boolean fallbackAllowed;

    public OpenRouterException(
            int statusCode,
            String errorCode,
            String message,
            boolean fallbackAllowed
    ) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.fallbackAllowed = fallbackAllowed;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isFallbackAllowed() {
        return fallbackAllowed;
    }
}