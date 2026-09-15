package com.saasai.dto;

import org.springframework.http.MediaType;

/**
 * ExportResult
 */
public record ExportResult(
        byte[] bytes,
        String fileName,
        MediaType mediaType
) {
}
