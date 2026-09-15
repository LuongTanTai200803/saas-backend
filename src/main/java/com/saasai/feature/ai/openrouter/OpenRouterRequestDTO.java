package com.saasai.feature.ai.openrouter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterRequestDTO(
        String model,

        List<OpenRouterMessageDTO> messages,

        Double temperature,

        @JsonProperty("max_tokens")
        Integer maxTokens,

        Boolean stream
) {
}