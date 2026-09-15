package com.saasai.admin;

import java.time.LocalDateTime;

public record TransactionFilter(
        String status,
        String userId,
        LocalDateTime from,
        LocalDateTime to
) {
}