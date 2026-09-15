package com.saasai.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadataResponseDTO {
    private String fileId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String category;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
}
