package com.saasai.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public enum DashboardRange {
    TODAY,
    DAYS_7,
    DAYS_30;

    public static DashboardRange parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "range phải là today, 7days hoặc 30days"
            );
        }

        return switch (value.trim().toLowerCase()) {
            case "today" -> TODAY;
            case "7days" -> DAYS_7;
            case "30days" -> DAYS_30;
            default -> throw new IllegalArgumentException(
                    "range không hợp lệ. Giá trị cho phép: today, 7days, 30days"
            );
        };
    }

    public LocalDateTime start(ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);

        return switch (this) {
            case TODAY -> today.atStartOfDay();
            case DAYS_7 -> today.minusDays(6).atStartOfDay();
            case DAYS_30 -> today.minusDays(29).atStartOfDay();
        };
    }

    public LocalDateTime end(ZoneId zoneId) {
        return LocalDate.now(zoneId)
                .plusDays(1)
                .atStartOfDay();
    }
    
    // Các phương thức dashboardStart và dashboardEnd được sử dụng để xác định khoảng thời gian hiển thị trên dashboard, khác với start và end dùng cho truy vấn dữ liệu thực tế.
    public LocalDateTime dashboardStart(ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);

        return switch (this) {
            case TODAY -> today.atStartOfDay();
            case DAYS_7 -> today.minusDays(7).atStartOfDay();
            case DAYS_30 -> today.minusDays(30).atStartOfDay();
        };
    }

    public LocalDateTime dashboardEnd(ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);

        return switch (this) {
            case TODAY -> today.plusDays(1).atStartOfDay();
            case DAYS_7, DAYS_30 -> today.atStartOfDay();
        };
    }

    public String apiValue() {
        return switch (this) {
            case TODAY -> "today";
            case DAYS_7 -> "7days";
            case DAYS_30 -> "30days";
        };
    }

}