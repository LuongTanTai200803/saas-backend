package com.saasai.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportRequestDTO {
    private String sessionUuid;
    private String exportFormat;
    private String htmlContent;
}
