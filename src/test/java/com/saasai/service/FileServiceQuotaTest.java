package com.saasai.service;

import com.saasai.admin.AdminService;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.FileMetadata; 
import com.saasai.entity.User;
import com.saasai.repository.FileMetadataRepository;
import com.saasai.repository.UserRepository;
import com.saasai.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class FileServiceQuotaTest {
    @Mock
    private FileMetadataRepository fileUploadRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminService adminService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private FileService fileService;

    private User user;
    private final String testUserId = "user-uuid-1"; // 🎯 ĐÃ SỬA

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(testUserId, null);
        authentication.setDetails("test@example.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AdminPackageConfig freePackage = AdminPackageConfig.builder()
                .packageType("FREE")
                .price(0L)
                .storageQuotaMb(1L)
                .build();

        user = User.builder()
                .userId(testUserId) // 🎯 ĐÃ SỬA
                .email("test@example.com")
                .fullName("Test User")
                .adminPackageConfig(freePackage) // 🎯 ĐÃ SỬA: Gán qua Object liên kết ngoại
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(adminService.getPackageConfig("FREE")).thenReturn(freePackage);
        when(fileUploadRepository.sumFileSizeByUserId(testUserId)).thenReturn(900L * 1024L); // 🎯 ĐÃ SỬA
    }

    @Test
    void uploadFile_shouldRejectWhenStorageQuotaExceeded() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", new byte[300 * 1024]);

        assertThatThrownBy(() -> fileService.uploadFile(file, "INPUT_DIRECTIVE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vượt quá hạn mức lưu trữ");
    }

    @Test
    void uploadFile_shouldUseDefaultQuotaWhenConfigQuotaMissing() throws IOException {
        AdminPackageConfig missingQuotaPackage = AdminPackageConfig.builder().packageType("FREE").price(0L).creditLimit(0.0).storageQuotaMb(null).build();
        user.setAdminPackageConfig(missingQuotaPackage);
        
        when(adminService.getPackageConfig("FREE")).thenReturn(missingQuotaPackage);
        when(storageService.storeFile(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn("/tmp/document.pdf");
        
        doAnswer(invocation -> {
            FileMetadata upload = invocation.getArgument(0);
            upload.setFileId("file-uuid-generations"); // 🎯 ĐÃ SỬA: id dạng String
            upload.setUploadedAt(LocalDateTime.now());
            return upload;
        }).when(fileUploadRepository).save(org.mockito.ArgumentMatchers.any(FileMetadata.class));

        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", new byte[300 * 1024]);

        assertThatNoException().isThrownBy(() -> fileService.uploadFile(file, "INPUT_DIRECTIVE"));
    }
}