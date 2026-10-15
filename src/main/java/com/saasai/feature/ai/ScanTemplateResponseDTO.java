package com.saasai.feature.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScanTemplateResponseDTO {

    private String sessionUuid;

    private boolean completed;

    private String templateDefinition;

    private AiProviderResultDTO aiResult;
}