package com.saasai.feature.ai.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterUsageDTO(
        @JsonProperty("prompt_tokens")
        Long promptTokens,

        @JsonProperty("completion_tokens")
        Long completionTokens,

        @JsonProperty("total_tokens")
        Long totalTokens,

        Double cost
) {
}