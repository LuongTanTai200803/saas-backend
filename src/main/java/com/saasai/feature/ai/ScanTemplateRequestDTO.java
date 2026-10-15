package com.saasai.feature.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScanTemplateRequestDTO {

    private String sessionUuid;

    private Boolean isLastBatch;
}