package com.saasai.feature.payment;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingInvoiceDTO {
    
    private Integer durationMonths;
    private String packageType;
    private String invoiceId;
    private String userId;
    private String memoId;
    private Long originalAmount;
    private Long discountAmount;
    private Long finalAmount;
    private String qrCodeUrl;
    private PaymentInfoDTO paymentInfo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime paymentDate;
}
