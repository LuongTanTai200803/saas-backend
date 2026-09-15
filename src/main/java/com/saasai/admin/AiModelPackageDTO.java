package com.saasai.admin;

public record AiModelPackageDTO(
        Long id,
        String code,
        String name,
        Double creditRate,
        String models,
        String description,
        Boolean active
) {
}