package com.saasai.feature.ai;

public record AiTemplateContent(
        String topicCode,
        String templateCode,
        String globalSystemInstruction,
        String topicPrompt,
        String templateContent
) {
}