package com.saasai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
// CREATE TABLE IF NOT EXISTS chat_sessions (
//     session_id BIGINT AUTO_INCREMENT PRIMARY KEY,   
//     session_uuid VARCHAR(36) NOT NULL,
//     user_id CHAR(36) NOT NULL,
//     session_name VARCHAR(255) NOT NULL DEFAULT 'Phiên làm việc mới',
//     tag_id VARCHAR(36),
//     status VARCHAR(50) DEFAULT 'DRAFT',
//     wizard_state_json JSON,
//     chat_history_json JSON,
//     editor_content LONGTEXT,
//     html_content LONGTEXT,
//     export_format VARCHAR(50),
//     created_at DATETIME NOT NULL,
//     updated_at DATETIME NOT NULL,
//     FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
//     INDEX idx_chat_sessions_user_id (user_id)
// );

import com.saasai.entity.ChatSession.SessionStatus;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id" )
    private Integer sessionId;

    @Column(name = "session_uuid", unique = true, nullable = false, updatable = false, length = 36)
    private String sessionUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_name", nullable = false)
    private String sessionName;

    @Column(name = "tag_id", length = 36)
    private String tagId;

    @Column(name = "editor_content", columnDefinition = "LONGTEXT")
    private String editorContent;

    @Column(name = "html_content", columnDefinition = "LONGTEXT")
    private String htmlContent;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @Column(name = "wizard_state_json", columnDefinition = "JSON")
    private String wizardStateJson;

    @Column(name = "chat_history_json", columnDefinition = "JSON")
    private String chatHistoryJson;

    @Column(name = "export_format")
    private String exportFormat;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // 🎯 CHÍ MẠNG: Tự động gen chuỗi UUID ngẫu nhiên bằng Java trước khi bản ghi
        // được lưu xuống MySQL
        if (this.sessionUuid == null) {
            this.sessionUuid = java.util.UUID.randomUUID().toString();
        }
        if (this.sessionName == null) {
            this.sessionName = "Phiên làm việc mới";
        }
        if (this.status == null) {
            this.status = SessionStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SessionStatus {
        DRAFT, EDITING, COMPLETED, ARCHIVED, CLOSED, ACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "assistant_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_chat_sessions_assistant")
    )
    private Assistant assistant;


    
}
