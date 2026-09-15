package com.saasai.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionCreateRequestDTO {
    
    private Integer assistantId;
    private String sessionName;
    
}
