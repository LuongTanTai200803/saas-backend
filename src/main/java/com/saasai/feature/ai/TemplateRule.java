package com.saasai.feature.ai;

import java.util.List;

public record TemplateRule(
        String templateCode,
        Integer priority,
        List<String> titleKeywords,
        List<String> requiredKeywords,
        List<String> excludedKeywords
) {
}