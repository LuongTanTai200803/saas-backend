package com.saasai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkGoogleRequestDTO {
    @NotBlank
    private String googleIdToken;
}