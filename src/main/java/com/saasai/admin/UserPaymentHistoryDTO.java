package com.saasai.admin;

import java.time.LocalDateTime;

public record UserPaymentHistoryDTO(
        String invoiceId,
        String packageType,
        Long amount,
        String status,
        String memoId,
        LocalDateTime createdAt,
        LocalDateTime paymentDate
) {
}