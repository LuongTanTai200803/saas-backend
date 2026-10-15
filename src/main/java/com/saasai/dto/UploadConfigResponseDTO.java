package com.saasai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class UploadConfigResponseDTO {
    private String packageType;
    private long maxFileSize;
    private int maxFiles;
    
    private long maxText;
    private List<String> allowedExtensions;

    private long storageQuota;
    private long usedStorage;
    private long remainingStorage;
}