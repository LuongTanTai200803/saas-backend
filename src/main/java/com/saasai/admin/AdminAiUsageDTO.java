package com.saasai.admin;

import java.time.LocalDateTime;

public record AdminAiUsageDTO(
        String range,
        LocalDateTime from,
        LocalDateTime to,
        long transactionCount,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        double creditsConsumed
) {
}