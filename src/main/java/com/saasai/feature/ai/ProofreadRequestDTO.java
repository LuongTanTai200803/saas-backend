package com.saasai.feature.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProofreadRequestDTO(
        @NotBlank String sessionUuid,
        @NotBlank @Size(max = 20000) String text) {
}