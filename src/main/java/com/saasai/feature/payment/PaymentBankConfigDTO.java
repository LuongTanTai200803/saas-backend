package com.saasai.feature.payment;

import java.time.LocalDateTime;

public record PaymentBankConfigDTO(
    Long id,
    String bankCode,
    String accountNumber,
    String accountName,
    String vaNumber,
    String template,
    Boolean showInfo,
    String store,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}