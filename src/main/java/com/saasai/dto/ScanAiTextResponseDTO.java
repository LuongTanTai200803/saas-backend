package com.saasai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScanAiTextResponseDTO {

    private String fileId;

    private boolean completed;

    private String fullText;

    private String extractionStatus;
}