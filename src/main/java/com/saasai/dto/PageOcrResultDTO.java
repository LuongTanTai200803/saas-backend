package com.saasai.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageOcrResultDTO {
    Integer pageIndex;
    String text;
}