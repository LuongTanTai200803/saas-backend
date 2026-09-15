package com.saasai.feature.payment;

import lombok.Data;

@Data
public class PaymentBankConfigRequest {
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String vaNumber;
    private String template;
    private Boolean showInfo;
    private String store;
    private Boolean isActive;
}