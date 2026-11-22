package com.saasai.feature.ai;

import jakarta.validation.constraints.NotBlank;

public record SelectSystemTemplateRequestDTO(
        @NotBlank String sessionUuid,
        @NotBlank String topicCode,
        @NotBlank String templateCode) {
}