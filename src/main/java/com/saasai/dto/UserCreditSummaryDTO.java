package com.saasai.dto;

import lombok.*;
import java.util.List;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreditSummaryDTO {

    private String userId;
    private String packageType;
    private LocalDateTime subscriptionExpireDate;

    private MonthlyCreditDTO monthly;
    private PurchasedCreditDTO purchased;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyCreditDTO {
        private Double allocated;
        private Double remaining;
        private LocalDateTime cycleStart;
        private LocalDateTime cycleEnd;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchasedCreditDTO {
        private Double balance;
        private LocalDateTime purchasedAt;
        private LocalDateTime expireAt;
    }

    // thêm nested DTO
    public static record InvoiceDTO(String invoiceId, String status, Long amount, LocalDateTime createdAt, String type) {}

    // ở builder DTO
    private List<InvoiceDTO> recentInvoices;
    private long aiCallCount;

}