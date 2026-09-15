package com.saasai.exception;

import com.saasai.feature.ai.ApiResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleAuthException(AuthException ex) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.UNAUTHORIZED;
        if (status.is4xxClientError()) {
            logger.warn("Authentication failure: {}", ex.getMessage());
        } else {
            logger.error("Authentication exception with unexpected status {}: {}", status, ex.getMessage());
        }
        return ResponseEntity.status(status)
                .body(ApiResponseDTO.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .statusCode(status.value())
                        .errorType(ex.getClass().getSimpleName())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDTO.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .statusCode(HttpStatus.FORBIDDEN.value())
                        .errorType(ex.getClass().getSimpleName())
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .errorType(ex.getClass().getSimpleName())
                        .build());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleNoSuchElementException(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .errorType(ex.getClass().getSimpleName())
                        .build());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleTooManyRequestsException(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponseDTO.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                        .errorType(ex.getClass().getSimpleName())
                        .build());
    }

    // Quan trọng: map rõ lỗi OpenRouter thay vì rơi vào 500 generic
    @ExceptionHandler(OpenRouterException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleOpenRouterException(OpenRouterException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        if (status.is4xxClientError()) {
            logger.warn(
                    "OpenRouter client error: statusCode={}, errorCode={}, fallbackAllowed={}, message={}",
                    ex.getStatusCode(), ex.getErrorCode(), ex.isFallbackAllowed(), ex.getMessage()
            );
        } else {
            logger.error(
                    "OpenRouter upstream error: statusCode={}, errorCode={}, fallbackAllowed={}, message={}",
                    ex.getStatusCode(), ex.getErrorCode(), ex.isFallbackAllowed(), ex.getMessage()
            );
        }

        return ResponseEntity.status(status)
                .body(ApiResponseDTO.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .statusCode(status.value())
                        .build());
    }

    // Optional nhưng hữu ích: URL sai trả rõ ràng 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleNoResourceFound(NoResourceFoundException ex) {
        logger.warn("No resource found: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.builder()
                        .success(false)
                        .message("Endpoint không tồn tại")
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleGeneralException(Exception ex) {
        logger.error("Unhandled exception caught in GlobalExceptionHandler", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.builder()
                        .success(false)
                        .message("Internal server error")
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build());
    }

    @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiResponseDTO<Object>> handleResponseStatusException(
                ResponseStatusException ex) {

        HttpStatusCode statusCode = ex.getStatusCode();

        logger.warn(
                "ResponseStatusException: status={}, reason={}",
                statusCode.value(),
                ex.getReason()
        );

        return ResponseEntity.status(statusCode)
                .body(ApiResponseDTO.builder()
                        .success(false)
                        .message(ex.getReason())
                        .statusCode(statusCode.value())
                        .errorType(ex.getClass().getSimpleName())
                        .build());
        }
}