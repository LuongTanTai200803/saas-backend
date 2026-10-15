package com.saasai.feature.ai.openrouter;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterVisionMessageDTO(
        String role,
        Object content
) {
}