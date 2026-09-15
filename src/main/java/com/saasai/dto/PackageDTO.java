package com.saasai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PackageDTO(
    Long id,
    String packageType,
    String packageCategory,
    Long price,
    String displayPrice,
    Double creditLimit,
    Integer duration,
    String durationHuman,
    String description,
    Long storageQuotaMb,
    Boolean isFree,
    Boolean canPurchase,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String badge
) {}