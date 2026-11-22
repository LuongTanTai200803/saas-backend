package com.saasai.feature.ai;

public record ProofreadResponseDTO(
        String correctedText,
        String model,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        double creditsDeducted) {
}