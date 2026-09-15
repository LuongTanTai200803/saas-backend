package com.saasai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_session_files",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_file_field",
                        columnNames = {
                                "session_id",
                                "file_id",
                                "field_code"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Session sử dụng file
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_session_files_session")
    )
    private ChatSession chatSession;

    /**
     * File được sử dụng trong session
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "file_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_session_files_file")
    )
    private FileMetadata file;

    /**
     * Field của FormData
     * Ví dụ:
     * MAIN_CONTENT
     * LEGAL_BASIS
     * STATISTICS
     */
        @Enumerated(EnumType.STRING)
        @Column(name = "field_code", nullable = false, length = 100)
        private PromptFieldCode fieldCode;

    /**
     * Nếu một field có nhiều file thì giữ đúng thứ tự FE upload
     */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public enum PromptFieldCode {

        MAIN_CONTENT,

        LEGAL_BASIS,

        DIRECTIVE, 

        STATISTICS,

        REFERENCE,

        OUTLINE,

        TEMPLATE
        }

}