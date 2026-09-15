package com.saasai.admin;

import java.time.LocalDateTime;

public record UserAdminDTO(
    String userId,
    String email,
    String packageType,
    LocalDateTime expireDate,
    Double creditsRemaining,
    String status // e.g. ACTIVE, SUSPENDED
) {}