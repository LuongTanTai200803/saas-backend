package com.saasai.feature.ai;

public record ResolvedTemplate(
                String templateFileName,
                String templateContent,
                String groupPrompt) {
}
