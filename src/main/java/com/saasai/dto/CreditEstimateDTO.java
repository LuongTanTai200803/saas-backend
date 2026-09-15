package com.saasai.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditEstimateDTO {
    private String modelName;
    private List<String> features;
    private String fileId;

    private Double inputLength;
    private String outputOption;
    private String modelSelected;
}
