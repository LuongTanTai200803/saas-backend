package com.saasai.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminRevenueDTO(
        String range,
        LocalDateTime from,
        LocalDateTime to,
        long totalRevenue,
        long paidInvoiceCount,
        List<AdminDashboardPointDTO> points
) {
}