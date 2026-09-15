package com.saasai.admin;

import java.time.LocalDateTime;

public record InvoiceDTO(
    String invoiceId,
    String userId,
    String packageType,
    Long finalAmount,
    String status,
    String memoId,
    String qrCodeUrl,
    LocalDateTime createdAt,
    LocalDateTime paymentDate
) {}