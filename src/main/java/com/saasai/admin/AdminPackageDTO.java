package com.saasai.admin;

public record AdminPackageDTO(
    Long id,
    String packageType,
    String packageCategory,
    Long price,
    Double creditLimit,
    Integer duration,
    String description,
    Long storageQuotaMb
) {}