package com.saasai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Entity;

@Entity
@Table(
        name = "file_ocr_pages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_file_ocr_page",
                columnNames = {"file_id", "page_index"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileOcrPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(name = "page_index", nullable = false)
    private Integer pageIndex;

    @Column(name = "ocr_text", columnDefinition = "LONGTEXT")
    private String ocrText;

    @Column(name = "ocr_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OcrStatus ocrStatus;

    public enum OcrStatus {
        EXTRACTED,
        FAILED
    }
}