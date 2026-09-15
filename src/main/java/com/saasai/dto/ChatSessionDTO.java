package com.saasai.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionDTO {
    private String sessionUuid;
    private String tagId;
    private String sessionName;
    private String currentEditorContent;
    private LocalDateTime createdAt;
    private String editorContent;
    private Integer assistantId;

    
}
