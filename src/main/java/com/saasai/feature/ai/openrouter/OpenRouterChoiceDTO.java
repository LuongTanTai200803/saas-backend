package com.saasai.feature.ai.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterChoiceDTO(
        Integer index,

        OpenRouterMessageDTO message,

        @JsonProperty("finish_reason")
        String finishReason,

        @JsonProperty("native_finish_reason")
        String nativeFinishReason,

        OpenRouterChoiceErrorDTO error
) {
}