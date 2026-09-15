package com.saasai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.dto.CreditEstimateDTO;
import com.saasai.dto.CreditEstimateResponseDTO;
import com.saasai.service.CreditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CreditControllerTest {

    @Mock
    private CreditService creditService;

    @InjectMocks
    private CreditController creditController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(creditController).build();
    }

    @Test
    void estimateCreditShouldReturnEstimate() throws Exception {
        CreditEstimateResponseDTO response = CreditEstimateResponseDTO.builder()
            .estimatedCredits(6.5)
            .currentCredits(10.0)
            .isEligible(true)
                .build();

        when(creditService.estimateCredits(any(CreditEstimateDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/credits/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                    "modelName", "claude-sonnet-4.6",
                    "features", new String[] { "LEGAL_REVIEW", "EXPORT_DOCX" },
                    "fileId", "file_15"
                        ))))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Ước tính credit thành công"))
            .andExpect(jsonPath("$.data.estimatedCredits").value(6.5))
            .andExpect(jsonPath("$.data.currentCredits").value(10.0))
            .andExpect(jsonPath("$.data.isEligible").value(true));
    }
}
