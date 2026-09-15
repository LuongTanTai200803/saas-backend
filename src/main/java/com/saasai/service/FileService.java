package com.saasai.service;

import com.saasai.admin.AdminService;
import com.saasai.dto.FileMetadataResponseDTO;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.ChatSession;
import com.saasai.entity.ChatSessionFile;
import com.saasai.entity.FileMetadata;
import com.saasai.entity.FileMetadata.ExtractionStatus;
import com.saasai.entity.User;
import com.saasai.extractor.ExtractResult;
import com.saasai.normalizer.TextNormalizer;
import com.saasai.repository.ChatSessionRepository;
import com.saasai.repository.FileMetadataRepository;
import com.saasai.repository.UserRepository;
import com.saasai.repository.redis.FileNormalizedTextRedisRepository;
import com.saasai.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("docx", "xlsx", "txt");

    private final FileMetadataRepository fileUploadRepository;
    private final UserRepository userRepository;
    private final AdminService adminService;
    private final StorageService storageService;
    private final FileExtractionService fileExtractionService;
    private final TextNormalizer textNormalizer;
    private final FileNormalizedTextRedisRepository fileTextRedisRepository;
    private final ChatSessionFileService chatSessionFileService;
    private final ChatSessionRepository chatSessionRepository;

    @Value("${storage.base-url:http://localhost:8080/uploads/}")
    private String storageBaseUrl;

    public FileService(
            FileMetadataRepository fileUploadRepository,
            UserRepository userRepository,
            AdminService adminService,
            StorageService storageService,
            FileExtractionService fileExtractionService,
            TextNormalizer textNormalizer,
            FileNormalizedTextRedisRepository fileTextRedisRepository,
            ChatSessionFileService chatSessionFileService,
            ChatSessionRepository chatSessionRepository
    ) {
        this.fileUploadRepository = fileUploadRepository;
        this.userRepository = userRepository;
        this.adminService = adminService;
        this.storageService = storageService;
        this.fileExtractionService = fileExtractionService;
        this.textNormalizer = textNormalizer;
        this.fileTextRedisRepository = fileTextRedisRepository;
        this.chatSessionFileService = chatSessionFileService;
        this.chatSessionRepository = chatSessionRepository;
    }
    
    // Uploads a file and associates it with the current user. 
    // Optionally, it can also attach the file to a chat session if sessionId is provided.
    public FileMetadataResponseDTO uploadFile(
        MultipartFile file,
        String category
    ) throws IOException {
        return uploadFile(file, category, null, null);
    }

    // Overloaded method to upload a file with sessionId but without fieldCode.
    public FileMetadataResponseDTO uploadFile(
        MultipartFile file,
        String category,
        String sessionUuid,
        String fieldCode
    ) throws IOException {
        
        User currentUser = userRepository.findByEmail(resolveCurrentEmail())
                .orElseThrow(() ->
                        new RuntimeException("Tài khoản không tồn tại!"));

        ChatSession session = chatSessionRepository
                .findBySessionUuidAndUser_UserId(sessionUuid, currentUser.getUserId())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Session không tồn tại hoặc không thuộc user"
                        )
                );

        Integer sessionId = session.getSessionId();

        validateFile(file);
        enforceStorageQuota(currentUser, file.getSize());

        String originalFileName = file.getOriginalFilename();
        String storedFileName = buildStoredFileName(originalFileName);
        FileMetadata.FileCategory fileCategory = normalizeCategory(category);

        storageService.storeFile(file, storedFileName);

        ExtractResult extractResult =
                extractContent(storedFileName, originalFileName);

        // Normalize the extracted raw text
        String rawText = extractResult.getRawText();
        
        String normalizedText = textNormalizer.normalize(rawText);

        FileMetadata fileUpload = FileMetadata.builder()
                .user(currentUser)
                .fileName(originalFileName)
                .fileUrl(storageBaseUrl + storedFileName)
                .fileSize(file.getSize())
                .category(fileCategory)
                .mimeType(file.getContentType())
                .rawText(rawText)
                .normalizedText(normalizedText)
                .characterCount(normalizedText.length())
                .wordCount(countWords(normalizedText))
                .extractionStatus(ExtractionStatus.EXTRACTED)
                .build();

        FileMetadata saved = fileUploadRepository.save(fileUpload);
        
        try {
            fileTextRedisRepository.save(
                    saved.getFileId(),
                    normalizedText
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Không thể cache normalized text vào Redis cho fileId={}. "
                    + "Dữ liệu vẫn đã được lưu trong DB.",
                    saved.getFileId(),
                    exception
            );
        }


        if (sessionId != null) {
            chatSessionFileService.attachFileToSession(
                    sessionId,
                    currentUser.getUserId(),
                    saved.getFileId(),
                    fieldCode
            );
        }
        
        return FileMetadataResponseDTO.builder()
                .fileId(saved.getFileId())
                .fileName(saved.getFileName())
                .fileUrl(saved.getFileUrl())
                .fileSize(saved.getFileSize())
                .category(saved.getCategory() != null
                        ? saved.getCategory().name()
                        : null)
                .uploadedAt(saved.getUploadedAt())
                .uploadedBy(currentUser.getFullName())
                .build();
    }

    // Normalize the fileId by removing "file_" prefix if present and validating it as a UUID.
    private String normalizeFileId(String rawFileId) {

        if (rawFileId == null || rawFileId.isBlank()) {
            throw new IllegalArgumentException("fileId không được để trống");
        }

        String fileId = rawFileId.startsWith("file_")
                ? rawFileId.substring(5)
                : rawFileId;

            try {
                UUID.fromString(fileId);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("fileId không hợp lệ: " + rawFileId);
            }

        return fileId;
    }
    

    // Helper method to count words in a given text. Returns 0 for null or blank text.
    private int countWords(String text) {

        if (text == null || text.isBlank()) {
            return 0;
        }

        return text.trim()
                .split("\\s+")
                .length;
    }

    private void enforceStorageQuota(User currentUser, Long incomingSize) {
        // 1. 🎯 ĐÃ SỬA: Lấy trực tiếp chuỗi String packageType từ Object liên kết, check null an toàn
        String packageType = (currentUser.getAdminPackageConfig() != null)
                ? currentUser.getAdminPackageConfig().getPackageType()
                : "FREE";
                
        // 2. Tìm cấu hình cấu trúc gói từ adminService
        AdminPackageConfig config = adminService.getPackageConfig(packageType);
        Long storageQuotaMb = config.getStorageQuotaMb();
        if (storageQuotaMb == null) {
            storageQuotaMb = getDefaultStorageQuotaMb(packageType); // Hàm bổ trợ của ông
        }

        long usedBytes = fileUploadRepository.sumFileSizeByUserId(currentUser.getUserId());
        long allowedBytes = storageQuotaMb * 1024L * 1024L;
        long totalBytes = usedBytes + (incomingSize != null ? incomingSize : 0L);
        if (totalBytes > allowedBytes) {
            throw new IllegalArgumentException(
                    "Vượt quá hạn mức lưu trữ của gói " + packageType + ". Hạn mức: " + storageQuotaMb + "MB.");
        }
    }

    private Long getDefaultStorageQuotaMb(String packageType) {
        return switch (packageType) {
            case "FREE", "BASIC" -> 100L;
            case "PROFESSIONAL" -> 1024L;
            case "ENTERPRISE" -> 5120L;
            default -> 100L;
        };
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload không được để trống");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ");
        }

        String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file docx, xlsx hoặc txt");
        }
    }

    private String buildStoredFileName(String originalFileName) {
        String safeFileName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return timestamp + "_" + safeFileName;
    }

    private String resolveCurrentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Không tìm thấy thông tin đăng nhập");
        }
        if (authentication.getDetails() instanceof String details && !details.isBlank()) {
            return details;
        }
        String name = authentication.getName();
        if (name != null && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
            return name;
        }
        throw new RuntimeException("Không tìm thấy email người dùng hiện tại");
    }

    private FileMetadata.FileCategory normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return FileMetadata.FileCategory.INPUT_DIRECTIVE;
        }

        String normalized = category.trim().toUpperCase();
        return switch (normalized) {
            case "INPUT_DIRECTIVE", "DIRECTIVE" -> FileMetadata.FileCategory.INPUT_DIRECTIVE;
            case "EVIDENCE" -> FileMetadata.FileCategory.EVIDENCE;
            case "LEGAL" -> FileMetadata.FileCategory.LEGAL;
            case "CONTENT" -> FileMetadata.FileCategory.CONTENT;
            case "TEMPLATE" -> FileMetadata.FileCategory.TEMPLATE;
            case "RELATED" -> FileMetadata.FileCategory.RELATED;
            case "OUTPUT_DOCUMENT", "OUTPUT" -> FileMetadata.FileCategory.OUTPUT_DOCUMENT;
            default -> FileMetadata.FileCategory.valueOf(normalized);
        };
    }

    // Extracts the content of a stored file using the FileExtractionService.
    // Throws an IOException if the file cannot be read or extracted.
    private ExtractResult extractContent(
        String storedFileName,
        String originalFileName
    ) throws IOException {

        java.nio.file.Path filePath = storageService.getFilePath(storedFileName);

        return fileExtractionService.extract(
                filePath,
                originalFileName
        );
    }

    // Resolves the PromptFieldCode enum from a string. Defaults to REFERENCE if the input is null, blank, or unrecognized.
    private ChatSessionFile.PromptFieldCode resolveFieldCode(String fieldCode) {
        if (fieldCode == null || fieldCode.isBlank()) {
            return ChatSessionFile.PromptFieldCode.REFERENCE;
        }

        return switch (fieldCode.trim().toUpperCase()) {
            case "MAIN_CONTENT" -> ChatSessionFile.PromptFieldCode.MAIN_CONTENT;
            case "LEGAL_BASIS" -> ChatSessionFile.PromptFieldCode.LEGAL_BASIS;
            case "DIRECTIVE" -> ChatSessionFile.PromptFieldCode.DIRECTIVE;
            case "STATISTICS" -> ChatSessionFile.PromptFieldCode.STATISTICS;
            case "REFERENCE" -> ChatSessionFile.PromptFieldCode.REFERENCE;
            case "OUTLINE" -> ChatSessionFile.PromptFieldCode.OUTLINE;
            case "TEMPLATE" -> ChatSessionFile.PromptFieldCode.TEMPLATE;
            default -> ChatSessionFile.PromptFieldCode.REFERENCE;
        };
    }
}