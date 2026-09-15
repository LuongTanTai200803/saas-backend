package com.saasai.feature.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingWebhookRequestDTO {
    private Long invoiceId;       // hệ thống nội bộ, có thể chưa dùng
    private String status;        // trạng thái nếu cần
    private String content;       // memo/content từ SePay
    private BigDecimal amount;    // số tiền
    private String transactionId; // ID giao dịch SePay
        
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionDate;

    public String getContent() {
        return content;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }
    public String getStatus() {
        return status;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
}
