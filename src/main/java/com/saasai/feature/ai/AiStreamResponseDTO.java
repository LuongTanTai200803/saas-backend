package com.saasai.feature.ai;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiStreamResponseDTO {
    private String type;
    private String text;
    private Double actualCreditDeducted;
    private Double refundedCredit;
    private Double currentBalance;
}
