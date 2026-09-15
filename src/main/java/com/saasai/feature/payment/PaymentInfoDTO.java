package com.saasai.feature.payment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentInfoDTO {
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private Long amount;
    private String additionalInfo;
}