package com.saasai.feature.ai;

import com.saasai.entity.ChatSession.SessionStatus;
import lombok.Builder;


@Builder
public record AiProviderResultDTO(
        String content,
        String model,
        String finishReason,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        SessionStatus sessionStatus
) {
}
