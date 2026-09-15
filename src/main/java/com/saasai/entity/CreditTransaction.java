package com.saasai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.saasai.entity.ChatSession;

@Entity
@Table(name = "credit_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", columnDefinition = "CHAR(36)")
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // =========================
    // AI Usage
    // =========================

    @Column(name = "model", length = 255)
    private String model;

    @Column(name = "model_package_id")
    private Long modelPackageId;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    // =========================
    // Credit pricing
    // =========================

    @Column(name = "input_credit")
    private Double inputCredit;

    @Column(name = "output_credit")
    private Double outputCredit;

    @Column(name = "credit_rate")
    private Double creditRate;

    @Column(name = "output_weight")
    private Double outputWeight;

    // =========================
    // Credit lifecycle
    // =========================

    @Column(name = "total_credit_hold")
    private Double totalCreditHold;

    @Column(name = "actual_credit_deducted")
    private Double actualCreditDeducted;

    @Column(name = "refunded_credit")
    private Double refundedCredit;

    // =========================
    // Transaction info
    // =========================

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private TransactionType type;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum TransactionType {
        TOPUP,
        HOLD,
        DEDUCT,
        REFUND
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ChatSession session;
}