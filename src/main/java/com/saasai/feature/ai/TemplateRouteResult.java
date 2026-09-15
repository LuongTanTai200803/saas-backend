package com.saasai.feature.ai;

public record TemplateRouteResult(
        String topicCode,
        String templateCode,
        boolean matchedByRule
) {
}