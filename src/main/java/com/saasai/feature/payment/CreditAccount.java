package com.saasai.feature.payment;

import com.saasai.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "credit_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditAccount {

    @Id
    @Column(name = "user_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String userId;

    @Column(name = "monthly_quota_allocated", nullable = false)
    private Double monthlyQuotaAllocated = 0.0;

    @Column(name = "monthly_quota_remaining", nullable = false)
    private Double monthlyQuotaRemaining = 0.0;

    @Column(name = "monthly_quota_cycle_start")
    private LocalDateTime monthlyQuotaCycleStart;

    @Column(name = "monthly_quota_cycle_end")
    private LocalDateTime monthlyQuotaCycleEnd;

    @Column(name = "purchased_credit_balance", nullable = false)
    private Double purchasedCreditBalance = 0.0;

    @Column(name = "purchased_credit_purchased_at")
    private LocalDateTime purchasedCreditPurchasedAt;

    @Column(name = "purchased_credit_expire_at")
    private LocalDateTime purchasedCreditExpireAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id"
    )
    private User user;
}
