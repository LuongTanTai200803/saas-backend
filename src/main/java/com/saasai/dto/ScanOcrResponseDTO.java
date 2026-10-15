package com.saasai.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ScanOcrResponseDTO {
    String fileId;
    List<PageOcrResultDTO> pages;
    List<Integer> failedPageIndexes;
    String fullText;
}