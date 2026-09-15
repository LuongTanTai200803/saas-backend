package com.saasai.feature.ai;


// Represents the routing configuration for an AI model, including primary and fallback models, token limits, temperature, model package ID, and credit rate.
public record ModelRoute(
    String primaryModel,
    String fallbackModel,
    Integer maxTokens,
    Double temperature,
    Long modelPackageId,
    Double creditRate
) {}