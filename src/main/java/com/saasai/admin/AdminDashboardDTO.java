package com.saasai.admin;

import java.time.LocalDate;

public record AdminDashboardDTO(
        long userCount,
        long activeUserCount,
        long transactionCount,
        long aiUsageCount,
        double creditsConsumed,
        long revenueToday,
        long revenueMonth,
        LocalDate asOfDate
) {
}