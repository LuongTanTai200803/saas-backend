package com.saasai.dto;

public record SessionFileOptionDTO(
        String fileId,
        String fileName,
        String extractionStatus,
        String extractionError
) {}