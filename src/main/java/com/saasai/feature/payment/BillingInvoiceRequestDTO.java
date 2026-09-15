package com.saasai.feature.payment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingInvoiceRequestDTO {
    private String packageType;
    private Integer durationMonths;
}
