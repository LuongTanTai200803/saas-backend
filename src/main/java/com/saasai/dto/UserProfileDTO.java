package com.saasai.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private String userId;
    private String email;
    private String fullName;
    private String agency;
    private String role;
    private String packageType;
    private String phone;
    private String position;
    private LocalDateTime created_at;
    private LocalDateTime expireDate;
    private AffiliateDTO affiliate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AffiliateDTO {
        private String code;
        private String link;
        private Double totalEarnings;
    }
}
