package com.saasai.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionWorkspaceDTO {
    private String sessionUuid;
    private String status;
    private String editorText;
    private List<ChatMessageDTO> messages;
}