package com.saasai.feature.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCompletionRequestDTO {

    private String sessionUuid;

    private String wizardStateJson;

    /**
     * Có thể để trống.
     *
     * FormData chính thức được lấy từ Draft
     * thông qua sessionId.
     */
    private String promptCommand;

    private Boolean pinEditorContext;

    /**
     * Backend tự chọn model theo package.
     */
    private String model;
    
    @JsonProperty("userText")
    @JsonAlias({"usertext"})
    private String userText;

}