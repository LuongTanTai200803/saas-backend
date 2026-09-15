package com.saasai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "invoice_id", columnDefinition = "CHAR(36)")
    private String invoiceId;

    // 🎯 ĐÃ SỬA: Thay đổi từ String sang liên kết @ManyToOne thực thụ trỏ sang
    // Entity User
    // Khớp hoàn toàn với CONSTRAINT fk_billing_invoices_user dưới DB
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Khớp hoàn toàn với CONSTRAINT fk_billing_invoices_package dưới DB
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private AdminPackageConfig adminPackageConfig;

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "original_amount")
    private Long originalAmount;

    @Column(name = "discount_amount")
    private Long discountAmount;

    @Column(name = "final_amount")
    private Long finalAmount;

    @Column(name = "memo_id")
    private String memoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50) // Khớp độ dài VARCHAR(50) của SQL
    private InvoiceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = InvoiceStatus.PENDING; // Tự động gán PENDING nếu code Service quên không set
        }
    }

    public enum InvoiceStatus {
        PENDING, PAID, CANCELLED, EXPIRED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", length = 50)
    private InvoiceType invoiceType;

    @Column(name = "package_snapshot", columnDefinition = "TEXT")
    private String packageSnapshot;

    public enum InvoiceType {
    SUBSCRIPTION, CREDIT_PACK
    }

    // inside entity fields
    @Column(name = "qr_code_url", length = 2048)
    private String qrCodeUrl;

    @Column(name = "qr_bank_snapshot", columnDefinition = "JSON")
    private String qrBankSnapshot; // store JSON snapshot of PaymentBankConfig used when creating invoice
}
