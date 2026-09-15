package com.saasai.admin;

public record AdminDashboardPointDTO(
        String label,
        long revenue,
        long totalTokens
) {
}