package com.saasai.feature.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponseDTO<T> {
    private Boolean success;
    private String message;
    private T data;
    private Integer statusCode;
    private String errorType;

    public static <T> ApiResponseDTO<T> success(T data) {
        return ApiResponseDTO.<T>builder()
                .success(true)
                .message("Thành công")
                .data(data)
                .statusCode(200)
                .build();
    }

    public static <T> ApiResponseDTO<T> success(
            String message,
            T data
    ) {
        return ApiResponseDTO.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .build();
    }

    public static <T> ApiResponseDTO<T> failure(String message, int statusCode, String errorType) {
        return ApiResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .errorType(errorType)
                .build();
    }

    public static <T> ApiResponseDTO<T> failure(String message) {
        return failure(message, 400, "BadRequest");
    }
}