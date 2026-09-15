package com.saasai.admin;

import java.time.LocalDateTime;

public record UserAiUsageDTO(
        String transactionId,
        String model,
        Long modelPackageId,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Double actualCreditDeducted,
        Double refundedCredit,
        String type,
        LocalDateTime createdAt
) {
}