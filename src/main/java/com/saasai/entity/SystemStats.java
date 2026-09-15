package com.saasai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_revenue")
    private Long totalRevenue;

    @Column(name = "new_users_count")
    private Long newUsersCount;

    @Column(name = "active_affiliates")
    private Long activeAffiliates;

    @Column(name = "total_credit_consumed")
    private Double totalCreditConsumed;

    @Column(name = "active_sessions_count")
    private Long activeSessionsCount;

    @Column(name = "total_documents_generated")
    private Long totalDocumentsGenerated;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
        if (this.totalRevenue == null)
            this.totalRevenue = 0L;
        if (this.newUsersCount == null)
            this.newUsersCount = 0L;
        if (this.activeAffiliates == null)
            this.activeAffiliates = 0L;
        if (this.totalCreditConsumed == null)
            this.totalCreditConsumed = 0.0;
        if (this.activeSessionsCount == null)
            this.activeSessionsCount = 0L;
        if (this.totalDocumentsGenerated == null)
            this.totalDocumentsGenerated = 0L;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}