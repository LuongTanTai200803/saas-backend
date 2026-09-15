package com.saasai.controller;

import com.saasai.feature.ai.ApiResponseDTO;
import com.saasai.dto.FileMetadataResponseDTO;
import com.saasai.service.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    private static final Long USER_ID = 10293L;

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileController fileController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(USER_ID, null);
        authentication.setDetails("user@example.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadFileShouldReturnFileMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "mock-content".getBytes()
        );

        FileMetadataResponseDTO response = FileMetadataResponseDTO.builder()
                .fileId("file_abc123")
                .fileName("test.pdf")
                .fileUrl("https://storage.trolyai.vn/inputs/test.pdf")
                .fileSize(1200L)
                .category("LEGAL")
                .uploadedAt(LocalDateTime.now())
                .uploadedBy("Nguyễn Văn A")
                .build();

            when(fileService.uploadFile(any(), eq("LEGAL")))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("category", "LEGAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tải file thành công"))
                .andExpect(jsonPath("$.data.fileId").value("file_abc123"))
                .andExpect(jsonPath("$.data.fileName").value("test.pdf"))
                .andExpect(jsonPath("$.data.category").value("LEGAL"))
                .andExpect(jsonPath("$.data.uploadedBy").value("Nguyễn Văn A"));
    }
}
