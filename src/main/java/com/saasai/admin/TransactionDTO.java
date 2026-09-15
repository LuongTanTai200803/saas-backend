package com.saasai.admin;

import java.time.LocalDateTime;

public record TransactionDTO(
    Long id,
    String invoiceId,
    String externalTransactionId,
    Long amount,
    String status,
    LocalDateTime createdAt
) {}