package com.saasai.feature.ai;

public record AiTemplateContent(
        String topicCode,
        String templateCode,
        String templateFileName,
        String commonPrompt,
        String assistPrompt,
        String topicPrompt,
        String templateContent,
        String groupPrompt) {
}