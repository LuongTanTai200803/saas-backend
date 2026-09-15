package com.saasai.feature.ai.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterErrorDTO(
        Integer code,
        String message,
        Object metadata
) {
}