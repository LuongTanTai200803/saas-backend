package com.saasai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "file_id", columnDefinition = "CHAR(36)")
    private String fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FileCategory category;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(name = "normalized_text", columnDefinition = "LONGTEXT")
    private String normalizedText;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    @Column(name = "character_count", nullable = false)
    private Integer characterCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false)
    private ExtractionStatus extractionStatus = ExtractionStatus.UPLOADED;

    public enum ExtractionStatus {
        UPLOADED,
        PROCESSING,
        EXTRACTED,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        if (this.category == null) {
            this.category = FileCategory.INPUT_DIRECTIVE;
        }
    }

    public enum FileCategory {
        INPUT_DIRECTIVE,
        EVIDENCE,
        LEGAL,
        CONTENT,
        TEMPLATE,
        RELATED,
        OUTPUT_DOCUMENT
    }
}