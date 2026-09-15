package com.saasai.admin;

public record AdminTopAssistantDTO(
        Integer assistantId,
        String name,
        long totalTokens,
        double creditsConsumed,
        double usagePercent
) {
}