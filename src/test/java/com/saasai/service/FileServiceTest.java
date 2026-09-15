package com.saasai.service;

import com.saasai.admin.AdminService;
import com.saasai.dto.FileMetadataResponseDTO;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.FileMetadata; // 🎯 ĐÃ SỬA
import com.saasai.entity.User;

import com.saasai.repository.FileMetadataRepository;
import com.saasai.repository.UserRepository;
import com.saasai.storage.StorageService;
import com.saasai.repository.redis.FileNormalizedTextRedisRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.saasai.repository.redis.FileNormalizedTextRedisRepository;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminService adminService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private FileService fileService;

    @Mock
    private FileNormalizedTextRedisRepository fileTextRedisRepository;

    @Mock
    private FileMetadataRepository fileUploadRepository;

    @TempDir
    Path tempDir;

    private User currentUser;
    private final String testUserId = "user-uuid-10293"; // 🎯 ĐÃ SỬA

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "storageBaseUrl", tempDir.toUri().toString());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(testUserId, null);
        authentication.setDetails("user@example.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AdminPackageConfig freePackage = AdminPackageConfig.builder()
                .packageType("FREE")
                .price(0L)
                .creditLimit(0.0)
                .storageQuotaMb(100L)
                .build();

        currentUser = User.builder()
                .userId(testUserId) // 🎯 ĐÃ SỬA
                .email("user@example.com")
                .fullName("Nguyễn Văn A")
                .adminPackageConfig(freePackage) // 🎯 ĐÃ SỬA
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(currentUser));
        lenient().when(adminService.getPackageConfig("FREE")).thenReturn(freePackage);
        lenient().when(fileMetadataRepository.sumFileSizeByUserId(testUserId)).thenReturn(0L); // 🎯 ĐÃ SỬA
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadFileShouldThrowWhenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileService.uploadFile(file, "LEGAL"));

        assertTrue(exception.getMessage().contains("trống"));
    }

    @Test
    void uploadFileShouldThrowWhenExtensionIsInvalid() {
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "abc".getBytes());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileService.uploadFile(file, "LEGAL"));

        assertTrue(exception.getMessage().contains("pdf, docx hoặc txt"));
    }

    @Test
    void uploadFileShouldSaveMetadataAndReturnDto() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "mock-content".getBytes());

        User mockUser = User.builder()
            .userId(testUserId) // Hệ String UUID
            .fullName("Nguyễn Văn A")
            .build();

        when(storageService.storeFile(any(), anyString())).thenReturn(tempDir.resolve("test.pdf").toString());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> {
            FileMetadata upload = invocation.getArgument(0);
            upload.setFileId("file-uuid-15"); 
            upload.setUploadedAt(LocalDateTime.now());

            return upload;
        });

        FileMetadataResponseDTO response = fileService.uploadFile(file, "LEGAL");

        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(fileMetadataRepository).save(captor.capture());
        FileMetadata saved = captor.getValue();

        assertEquals(testUserId, saved.getUser() != null ? saved.getUser().getUserId() : null); 
        assertEquals("test.pdf", saved.getFileName());
        assertEquals(FileMetadata.FileCategory.LEGAL, saved.getCategory()); //
        verify(storageService).storeFile(any(), anyString());
        assertEquals("file_file-uuid-15", response.getFileId()); //
        assertEquals("Nguyễn Văn A", response.getUploadedBy());
    }
}