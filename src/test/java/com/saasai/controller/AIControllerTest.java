package com.saasai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.feature.ai.AiStreamResponseDTO;
import com.saasai.feature.ai.AiController;
import com.saasai.feature.ai.AiService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AIControllerTest {

    private static final String USER_ID = "user-uuid-10293";

    @Mock
    private AiService aiService;

    @InjectMocks
    private AiController aiController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiController).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completionsShouldStreamSseEvents() throws Exception {
        doAnswer(invocation -> {
                    SseEmitter emitter = invocation.getArgument(6);
                    emitter.send(AiStreamResponseDTO.builder().type("content").text("chunk-1").build());
                    emitter.send(AiStreamResponseDTO.builder()
                            .type("verify_done")
                            .actualCreditDeducted(4.1)
                            .refundedCredit(0.4)
                            .currentBalance(345.9)
                            .build());
                    emitter.complete();
                    return null;
                }).when(aiService)
                .processCompletion(eq(502), eq(USER_ID), eq("{}"), eq("Rewrite"), eq(true), eq("claude-sonnet-4.6"), any());

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/ai/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", "502",
                                "wizardStateJson", "{}",
                                "promptCommand", "Rewrite",
                                "pinEditorContext", true,
                                "model", "claude-sonnet-4.6"
                        ))))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult(5000);

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"type\":\"content\"")))
                .andExpect(content().string(containsString("\"type\":\"verify_done\"")));
    }

    @Test
    void exportShouldReturnBinaryFile() throws Exception {
        byte[] fileContent = "mock-file-content".getBytes(StandardCharsets.UTF_8);

        when(aiService.exportDocument(eq("502"), eq(USER_ID), eq("DOCX"))).thenReturn(fileContent);

        mockMvc.perform(post("/api/v1/ai/exporter/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", "502",
                                "exportFormat", "DOCX",
                                "htmlContent", "<h1>Document</h1>"
                        ))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"document.docx\""))
                .andExpect(content().bytes(fileContent));
    }
}
