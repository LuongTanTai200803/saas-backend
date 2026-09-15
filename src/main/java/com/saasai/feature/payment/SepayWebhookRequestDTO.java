package com.saasai.feature.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.saasai.config.SePayDateTimeDeserializer;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
public class SepayWebhookRequestDTO {

    private Long id;                    // ID giao dịch SePay

    @JsonDeserialize(using = SePayDateTimeDeserializer.class)
    private LocalDateTime transactionDate;   // ngày giờ giao dịch SePay

    private String accountNumber;       // số tài khoản SePay

    private String code;                // mã code từ SePay

    private String content;             // nội dung chuyển khoản

    private String description;         // mô tả giao dịch

    private String transferType;        // "in" hoặc "out"

    @JsonProperty("transferAmount")
    private BigDecimal transferAmount;  // số tiền giao dịch

    private String referenceCode;       // mã tham chiếu giao dịch SePay
}