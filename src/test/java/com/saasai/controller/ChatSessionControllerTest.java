package com.saasai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.dto.ChatSessionDTO;
import com.saasai.service.ChatSessionService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatSessionControllerTest {

    private static final String USER_ID = "10293";

    @Mock
    private ChatSessionService chatSessionService;

    @InjectMocks
    private ChatSessionController chatSessionController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatSessionController).build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(USER_ID, null);
        authentication.setDetails("user@example.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSessionShouldReturnCreated() throws Exception {
        ChatSessionDTO session = ChatSessionDTO.builder()
                .sessionUuid("502")
                .assistantId(1)
                .sessionName("Khởi tạo văn bản mới")
                .currentEditorContent("")
                .createdAt(LocalDateTime.now())
                .build();

        when(chatSessionService.createSession(eq("user@example.com"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(session);

        mockMvc.perform(post("/api/v1/chat-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tagId", "dang_tinh_uy",
                                "sessionName", "Khởi tạo văn bản mới"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionUuid").value("502"))
                .andExpect(jsonPath("$.assistantId").value(1));
    }

    @Test
    void updateEditorContentShouldReturnSuccess() throws Exception {
        doNothing().when(chatSessionService).updateEditorContent("502", "user@example.com", "<p>Nội dung</p>");

        mockMvc.perform(put("/api/v1/chat-sessions/502/editor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("htmlContent", "<p>Nội dung</p>"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã lưu bản nháp văn bản thành công"));

        verify(chatSessionService).updateEditorContent("502", "user@example.com", "<p>Nội dung</p>");
    }
}
