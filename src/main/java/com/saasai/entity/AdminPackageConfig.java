package com.saasai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPackageConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Loại gói (ví dụ: FREE, BASIC, PROFESSIONAL, ENTERPRISE)
    @Column(name = "package_type", unique = true, nullable = false)
    private String packageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_category", length = 50, nullable = false)
    private PackageCategory packageCategory; // SUBSCRIPTION or CREDIT_PACK

    @Column(nullable = false)
    private Long price;

    @Column(name = "credit_limit", nullable = false)
    private Double creditLimit;

    /**
     * Duration semantics:
     * - For SUBSCRIPTION: duration is months (e.g. 30 -> you may interpret as days if you prefer).
     * - For CREDIT_PACK: duration is days (expiry window for purchased credits).
     */
    @Column(name = "duration", nullable = false)
    private Integer duration;

    /**
     * Maximum allowed AI model package level.
     * e.g. 1, 2, 3 (max allowed model package level)
     */
    @Column(name = "model_package_level")
    private Integer modelPackageLevel; 

    private String description;

    private Long storageQuotaMb;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (storageQuotaMb == null) {
            storageQuotaMb = defaultStorageQuotaMb();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (storageQuotaMb == null) {
            storageQuotaMb = defaultStorageQuotaMb();
        }
    }

    private Long defaultStorageQuotaMb() {
        if (packageType == null)
            return 100L;
        return switch (packageType.toUpperCase()) {
            case "FREE", "BASIC" -> 100L;
            case "PROFESSIONAL" -> 1024L;
            case "ENTERPRISE" -> 5120L;
            default -> 100L;
        };
    }

    public enum PackageCategory {
        SUBSCRIPTION,
        CREDIT_PACK
    }
}