package com.saasai.service;

import com.saasai.admin.AdminService;
import com.saasai.dto.FileMetadataResponseDTO;
import com.saasai.dto.PageOcrResultDTO;
import com.saasai.dto.ScanOcrResponseDTO;
import com.saasai.dto.UploadConfigResponseDTO;
import com.saasai.dto.ScanAiTextResponseDTO;

import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.ChatSession;
import com.saasai.entity.ChatSessionFile;
import com.saasai.entity.FileMetadata;
import com.saasai.entity.FileMetadata.ExtractionStatus;
import com.saasai.entity.FileOcrPage;
import com.saasai.entity.User;

import com.saasai.extractor.ExtractResult;
import com.saasai.feature.ai.AiCompletionService;
import com.saasai.normalizer.TextNormalizer;
import com.saasai.repository.ChatSessionFileRepository;
import com.saasai.repository.ChatSessionRepository;
import com.saasai.repository.FileMetadataRepository;
import com.saasai.repository.FileOcrPageRepository;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;

@Service
public class FileService {

        private static final Logger log = LoggerFactory.getLogger(FileService.class);

        private final FileMetadataRepository fileUploadRepository;
        private final UserRepository userRepository;
        private final AdminService adminService;
        private final StorageService storageService;
        private final FileExtractionService fileExtractionService;
        private final TextNormalizer textNormalizer;
        private final FileNormalizedTextRedisRepository fileTextRedisRepository;
        private final ChatSessionFileService chatSessionFileService;
        private final ChatSessionRepository chatSessionRepository;
        private final ObjectMapper objectMapper;
        private final ChatSessionFileRepository chatSessionFileRepository;
        private final VisionOcrService visionOcrService;
        private final FileOcrPageRepository fileOcrPageRepository;

        private final ScanAiTempStorage scanAiTempStorage;
        private final AiVisionService aiVisionService;

        private final TemplateProcessingService templateProcessingService;

        private final ScanAiProcessingService scanAiProcessingService;

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
                        ChatSessionRepository chatSessionRepository,
                        ObjectMapper objectMapper,
                        ChatSessionFileRepository chatSessionFileRepository,
                        VisionOcrService visionOcrService,
                        FileOcrPageRepository fileOcrPageRepository,
                        ScanAiTempStorage scanAiTempStorage,
                        AiVisionService aiVisionService,
                        TemplateProcessingService templateProcessingService,
                        ScanAiProcessingService scanAiProcessingService) {
                this.fileUploadRepository = fileUploadRepository;
                this.userRepository = userRepository;
                this.adminService = adminService;
                this.storageService = storageService;
                this.fileExtractionService = fileExtractionService;
                this.textNormalizer = textNormalizer;
                this.fileTextRedisRepository = fileTextRedisRepository;
                this.chatSessionFileService = chatSessionFileService;
                this.chatSessionRepository = chatSessionRepository;
                this.objectMapper = objectMapper;
                this.chatSessionFileRepository = chatSessionFileRepository;
                this.visionOcrService = visionOcrService;
                this.fileOcrPageRepository = fileOcrPageRepository;
                this.scanAiTempStorage = scanAiTempStorage;
                this.aiVisionService = aiVisionService;
                this.scanAiProcessingService = scanAiProcessingService;
                this.templateProcessingService = templateProcessingService;
        }

        // Hash the content of a file using SHA-256
        private String calculateContentHash(
                        MultipartFile file) throws IOException {
                try {
                        byte[] fileBytes = file.getBytes();

                        MessageDigest digest = MessageDigest.getInstance("SHA-256");

                        byte[] hashBytes = digest.digest(fileBytes);

                        StringBuilder hash = new StringBuilder();

                        for (byte hashByte : hashBytes) {
                                hash.append(String.format(
                                                "%02x",
                                                hashByte));
                        }

                        return hash.toString();

                } catch (NoSuchAlgorithmException exception) {
                        throw new IllegalStateException(
                                        "Không hỗ trợ thuật toán SHA-256",
                                        exception);
                }
        }

        // Overloaded method to upload a file with sessionId but without fieldCode.
        public FileMetadataResponseDTO uploadFile(
                        MultipartFile file,
                        String category,
                        String sessionUuid,
                        String fieldCode) throws IOException {

                User currentUser = userRepository
                                .findByEmail(resolveCurrentEmail())
                                .orElseThrow(() -> new RuntimeException(
                                                "Tài khoản không tồn tại!"));

                if (sessionUuid == null || sessionUuid.isBlank()) {
                        throw new IllegalArgumentException(
                                        "sessionUuid không được để trống");
                }

                // Check if the session exists and belongs to the current user.
                ChatSession session = chatSessionRepository
                                .findBySessionUuidAndUser_UserId(
                                                sessionUuid,
                                                currentUser.getUserId())
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Session không tồn tại hoặc không thuộc user"));

                Integer sessionId = session.getSessionId();

                AdminPackageConfig packageConfig = currentUser.getAdminPackageConfig() != null
                                ? currentUser.getAdminPackageConfig()
                                : adminService.getPackageConfig("FREE");

                validateFile(file, packageConfig);
                validateFileByPackage(file, packageConfig);

                // Calculate the content hash of the file to check for duplicates.
                String contentHash = calculateContentHash(file);

                FileMetadata.FileCategory fileCategory = normalizeCategory(category);

                // Tìm file đã tồn tại theo session, user và nội dung file.
                Optional<FileMetadata> existingFile = chatSessionFileRepository
                                .findBySessionAndUserAndContentHash(
                                                sessionId,
                                                currentUser.getUserId(),
                                                contentHash);

                FileMetadata retryingFile = null;
                String oldStoredFileName = null;

                // File chỉ được upload lại khi lần xử lý trước bị FAILED.
                if (existingFile.isPresent()) {
                        FileMetadata existing = existingFile.get();

                        if (existing.getExtractionStatus() != ExtractionStatus.FAILED) {
                                templateProcessingService.submit(
                                                existing.getFileId(),
                                                session.getSessionId(),
                                                currentUser.getUserId());

                                return toResponse(
                                                existing,
                                                currentUser,
                                                fieldCode);
                        }

                        // Lưu tên file vật lý cũ để xóa sau khi file mới được lưu thành công.
                        oldStoredFileName = extractStoredFileName(
                                        existing.getFileUrl());

                        // Giữ nguyên fileId và quan hệ với session.
                        // Xóa dữ liệu xử lý cũ để ghi kết quả mới.
                        existing.setExtractionStatus(
                                        ExtractionStatus.PROCESSING);
                        existing.setExtractionError(null);
                        existing.setRawText(null);
                        existing.setNormalizedText(null);
                        existing.setWordCount(0);
                        existing.setCharacterCount(0);
                        existing.setCategory(fileCategory);
                        existing.setFileName(file.getOriginalFilename());
                        existing.setFileSize(file.getSize());
                        existing.setMimeType(file.getContentType());
                        existing.setContentHash(contentHash);

                        retryingFile = existing;
                }

                // File retry đã được tính trong quota rồi, không cộng thêm lần nữa.
                if (retryingFile == null) {
                        enforceStorageQuota(
                                        currentUser,
                                        packageConfig,
                                        file.getSize());
                }

                String originalFileName = file.getOriginalFilename();
                String storedFileName = buildStoredFileName(
                                originalFileName);

                // Lưu file vật lý mới trước.
                storageService.storeFile(
                                file,
                                storedFileName);

                boolean isPdf = "pdf".equals(
                                getExtension(originalFileName));

                FileMetadata saved;

                if (retryingFile != null) {
                        // Retry: cập nhật record cũ và giữ nguyên fileId.
                        retryingFile.setFileName(originalFileName);
                        retryingFile.setFileSize(file.getSize());
                        retryingFile.setMimeType(file.getContentType());
                        retryingFile.setCategory(fileCategory);
                        retryingFile.setFileUrl(
                                        storageBaseUrl + storedFileName);

                        saved = fileUploadRepository.save(retryingFile);

                        // Xóa file vật lý cũ sau khi file mới đã được lưu thành công.
                        if (oldStoredFileName != null) {
                                try {
                                        storageService.deleteFile(
                                                        oldStoredFileName);
                                } catch (IOException exception) {
                                        log.warn(
                                                        "Không thể xóa file vật lý cũ: {}",
                                                        oldStoredFileName,
                                                        exception);
                                }
                        }
                } else {
                        // Upload mới: tạo fileId mới.
                        saved = fileUploadRepository.save(
                                        FileMetadata.builder()
                                                        .user(currentUser)
                                                        .fileName(originalFileName)
                                                        .fileUrl(storageBaseUrl + storedFileName)
                                                        .fileSize(file.getSize())
                                                        .category(fileCategory)
                                                        .mimeType(file.getContentType())
                                                        .contentHash(contentHash)
                                                        .rawText(null)
                                                        .normalizedText(null)
                                                        .wordCount(0)
                                                        .characterCount(0)
                                                        .extractionStatus(
                                                                        ExtractionStatus.UPLOADED)
                                                        .extractionError(null)
                                                        .build());

                        // Chỉ upload mới mới cần attach vào session.
                        chatSessionFileService.attachFileToSession(
                                        sessionId,
                                        currentUser.getUserId(),
                                        saved.getFileId(),
                                        fieldCode);
                }

                // PDF scan sẽ được xử lý ở endpoint OCR riêng.
                if (isPdf) {
                        saved.setExtractionStatus(
                                        ExtractionStatus.PROCESSING);
                        saved = fileUploadRepository.save(saved);

                        log.info(
                                        "File text extraction started: fileName={}, storedFileName={}",
                                        originalFileName,
                                        storedFileName);

                        ExtractResult result = extractContent(
                                        storedFileName,
                                        originalFileName);

                        String rawText = result.getRawText();

                        if (rawText != null && rawText.trim().length() >= 50) {
                                String normalizedText = textNormalizer.normalize(rawText);

                                saved.setRawText(rawText);
                                saved.setNormalizedText(normalizedText);
                                saved.setWordCount(countWords(normalizedText));
                                saved.setCharacterCount(normalizedText.length());
                                saved.setExtractionStatus(
                                                ExtractionStatus.EXTRACTED);

                                saved = fileUploadRepository.save(saved);

                                fileTextRedisRepository.save(
                                                saved.getFileId(),
                                                rawText);

                                templateProcessingService.submit(
                                                saved.getFileId(),
                                                session.getSessionId(),
                                                currentUser.getUserId());

                                return toResponse(saved, currentUser, fieldCode);
                        }

                        // PDF scan: frontend tiếp tục gửi ảnh theo patch
                        saved.setExtractionStatus(
                                        ExtractionStatus.PROCESSING);
                        fileUploadRepository.save(saved);

                        return toResponse(saved, currentUser, fieldCode);
                }

                /*
                 * Bước 3: chuyển sang PROCESSING.
                 */
                saved.setExtractionStatus(ExtractionStatus.PROCESSING);

                saved = fileUploadRepository.save(saved);

                try {
                        /*
                         * Bước 4: extract và normalize.
                         */
                        ExtractResult extractResult = extractContent(
                                        storedFileName,
                                        originalFileName);

                        String rawText = extractResult.getRawText();

                        if (rawText == null || rawText.isBlank()) {
                                throw new IllegalArgumentException(
                                                "Không thể trích xuất nội dung file");
                        }

                        String normalizedText = textNormalizer.normalize(rawText);

                        /*
                         * Bước 5: lưu nội dung và chuyển sang EXTRACTED.
                         */
                        saved.setRawText(rawText);
                        saved.setNormalizedText(normalizedText);
                        saved.setCharacterCount(
                                        normalizedText != null
                                                        ? normalizedText.length()
                                                        : 0);
                        saved.setWordCount(
                                        countWords(normalizedText));
                        saved.setExtractionStatus(
                                        ExtractionStatus.EXTRACTED);

                        saved = fileUploadRepository.save(saved);

                        templateProcessingService.submit(
                                        saved.getFileId(),
                                        session.getSessionId(),
                                        currentUser.getUserId());

                        /*
                         * Redis chỉ là cache.
                         * Redis lỗi không làm upload thất bại
                         * vì dữ liệu chính đã nằm trong database.
                         */
                        try {
                                fileTextRedisRepository.save(
                                                saved.getFileId(),
                                                rawText);
                        } catch (RuntimeException exception) {
                                log.warn(
                                                "Không thể cache normalized text "
                                                                + "cho fileId={}. Dữ liệu DB vẫn hợp lệ.",
                                                saved.getFileId(),
                                                exception);
                        }

                        /*
                         * HTTP 200 => frontend xử lý success.
                         */
                        return FileMetadataResponseDTO.builder()
                                        .fileId(saved.getFileId())
                                        .fileName(saved.getFileName())
                                        .fileUrl(saved.getFileUrl())
                                        .fileSize(saved.getFileSize())
                                        .category(
                                                        saved.getCategory() != null
                                                                        ? saved.getCategory().name()
                                                                        : null)
                                        .uploadedAt(saved.getUploadedAt())
                                        .uploadedBy(currentUser.getFullName())
                                        .extractionStatus(saved.getExtractionStatus().name())
                                        .extractionError(saved.getExtractionError())
                                        .rawText(
                                                        shouldExposeRawText(
                                                                        saved.getCategory(),
                                                                        fieldCode)
                                                                                        ? saved.getRawText()
                                                                                        : null)
                                        .build();

                } catch (Exception exception) {
                        /*
                         * Extract hoặc attach session lỗi:
                         * lưu trạng thái FAILED rồi trả lỗi cho frontend.
                         */
                        saved.setExtractionStatus(
                                        ExtractionStatus.FAILED);

                        fileUploadRepository.save(saved);

                        throw new IOException(
                                        "Không thể xử lý file: "
                                                        + originalFileName,
                                        exception);
                }
        }

        // Normalize the fileId by removing "file_" prefix if present and validating it
        // as a UUID.
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

        // Helper method to count words in a given text. Returns 0 for null or blank
        // text.
        private int countWords(String text) {

                if (text == null || text.isBlank()) {
                        return 0;
                }

                return text.trim()
                                .split("\\s+").length;
        }

        // Enforces the storage quota for the current user based on their package
        // configuration and the size of the incoming file. Throws an exception if the
        // quota is exceeded.
        private void enforceStorageQuota(
                        User currentUser,
                        AdminPackageConfig packageConfig,
                        Long incomingSize) {
                Long storageQuotaMb = packageConfig.getStorageQuotaMb();

                if (storageQuotaMb == null) {
                        storageQuotaMb = getDefaultStorageQuotaMb(
                                        packageConfig.getPackageType());
                }

                long usedBytes = fileUploadRepository.sumFileSizeByUserId(
                                currentUser.getUserId());

                long allowedBytes = storageQuotaMb * 1024L * 1024L;

                long totalBytes = usedBytes + (incomingSize != null ? incomingSize : 0L);

                if (totalBytes > allowedBytes) {
                        throw new IllegalArgumentException(
                                        "Vượt quá hạn mức lưu trữ của gói "
                                                        + packageConfig.getPackageType()
                                                        + ". Hạn mức: "
                                                        + storageQuotaMb
                                                        + "MB.");
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

        // Validates the uploaded file for null, emptiness, and proper file name format.
        // Throws an exception if the file is invalid.
        private void validateFile(
                        MultipartFile file,
                        AdminPackageConfig packageConfig) {
                if (file == null || file.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "File upload không được để trống");
                }

                long maxFileSizeMb = packageConfig.getMaxFileSizeMb() != null
                                ? packageConfig.getMaxFileSizeMb()
                                : 10L;

                long maxFileSizeBytes = maxFileSizeMb * 1024L * 1024L;

                if (file.getSize() > maxFileSizeBytes) {
                        throw new IllegalArgumentException(
                                        "Dung lượng file vượt quá giới hạn "
                                                        + maxFileSizeMb
                                                        + "MB");
                }

                String originalFileName = file.getOriginalFilename();

                if (originalFileName == null
                                || originalFileName.isBlank()
                                || !originalFileName.contains(".")) {
                        throw new IllegalArgumentException(
                                        "Định dạng file không hợp lệ");
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
                        String originalFileName) throws IOException {

                java.nio.file.Path filePath = storageService.getFilePath(storedFileName);

                return fileExtractionService.extract(
                                filePath,
                                originalFileName);
        }

        // Resolves the PromptFieldCode enum from a string. Defaults to REFERENCE if the
        // input is null, blank, or unrecognized.
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

        // Validates a file against the constraints defined in the given
        // AdminPackageConfig.
        private void validateFileByPackage(
                        MultipartFile file,
                        AdminPackageConfig packageConfig) {
                String extension = getExtension(file.getOriginalFilename());

                Set<String> allowedExtensions = parseAllowedFileTypes(
                                packageConfig.getAllowedFileTypes());

                if (!allowedExtensions.contains(extension)) {
                        throw new IllegalArgumentException(
                                        "Gói "
                                                        + packageConfig.getPackageType()
                                                        + " không hỗ trợ file ."
                                                        + extension
                                                        + ". Loại file được phép: "
                                                        + String.join(
                                                                        ", ",
                                                                        allowedExtensions));
                }
        }

        // Extracts the file extension from the given file name. Throws an exception if
        // the file name is null, blank, or does not have a valid extension.
        private String getExtension(String fileName) {
                if (fileName == null || fileName.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Tên file không được để trống");
                }

                int dotIndex = fileName.lastIndexOf('.');

                if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
                        throw new IllegalArgumentException(
                                        "File không có phần mở rộng hợp lệ");
                }

                return fileName
                                .substring(dotIndex + 1)
                                .trim()
                                .toLowerCase(Locale.ROOT);
        }

        // Parses the allowed file types from the given JSON string and returns them as
        // a set of normalized file extensions. Throws an exception if the input is
        // null, blank, or not a valid JSON array of strings.
        private Set<String> parseAllowedFileTypes(
                        String allowedFileTypes) {
                if (allowedFileTypes == null
                                || allowedFileTypes.isBlank()) {
                        throw new IllegalStateException(
                                        "Package chưa cấu hình loại file được phép");
                }

                try {
                        List<String> fileTypes = objectMapper.readValue(
                                        allowedFileTypes,
                                        objectMapper.getTypeFactory()
                                                        .constructCollectionType(
                                                                        List.class,
                                                                        String.class));

                        return fileTypes.stream()
                                        .filter(type -> type != null && !type.isBlank())
                                        .map(type -> type.trim()
                                                        .toLowerCase(Locale.ROOT)
                                                        .replaceFirst("^\\.", ""))
                                        .collect(Collectors.toCollection(
                                                        LinkedHashSet::new));

                } catch (JsonProcessingException exception) {
                        throw new IllegalStateException(
                                        "allowedFileTypes của package không hợp lệ",
                                        exception);
                }
        }

        // Processes OCR for scanned document images associated with a specific file and
        // session. Returns the OCR results for each page and a list of failed pages.
        public ScanOcrResponseDTO processScanOcr(
                        String rawFileId,
                        String sessionUuid,
                        List<MultipartFile> images,
                        List<Integer> pageIndexes,
                        boolean isLastBatch) throws IOException {

                // Validate input parameters for the OCR processing request.
                if (images == null
                                || pageIndexes == null
                                || images.isEmpty()
                                || images.size() > 5
                                || images.size() != pageIndexes.size()) {
                        throw new IllegalArgumentException(
                                        "Mỗi request phải gửi từ 1 đến 5 ảnh "
                                                        + "và pageIndexes tương ứng");
                }

                User currentUser = userRepository
                                .findByEmail(resolveCurrentEmail())
                                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

                ChatSession session = chatSessionRepository
                                .findBySessionUuidAndUser_UserId(
                                                sessionUuid,
                                                currentUser.getUserId())
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Session không tồn tại hoặc không thuộc user"));

                String fileId = normalizeFileId(rawFileId);

                FileMetadata file = fileUploadRepository
                                .findByFileIdAndUser_UserId(
                                                fileId,
                                                currentUser.getUserId())
                                .orElseThrow(() -> new NoSuchElementException(
                                                "File không tồn tại hoặc không thuộc user"));

                boolean attached = chatSessionFileRepository
                                .existsByChatSessionAndFile(
                                                session,
                                                file);

                if (!attached) {
                        throw new IllegalArgumentException(
                                        "File không thuộc session hiện tại");
                }

                for (int index = 0; index < images.size(); index++) {
                        MultipartFile image = images.get(index);
                        Integer pageIndex = pageIndexes.get(index);

                        validateOcrImage(image);
                        validatePageIndex(pageIndex);

                        try {
                                String text = visionOcrService.extractText(image);

                                FileOcrPage page = fileOcrPageRepository
                                                .findByFileAndPageIndex(file, pageIndex)
                                                .orElse(
                                                                FileOcrPage.builder()
                                                                                .file(file)
                                                                                .pageIndex(pageIndex)
                                                                                .build());

                                page.setOcrText(text == null ? "" : text);
                                page.setOcrStatus(
                                                FileOcrPage.OcrStatus.EXTRACTED);

                                fileOcrPageRepository.save(page);

                        } catch (Exception exception) {
                                log.error(
                                                "OCR thất bại fileId={}, pageIndex={}",
                                                file.getFileId(),
                                                pageIndex,
                                                exception);

                                FileOcrPage page = fileOcrPageRepository
                                                .findByFileAndPageIndex(file, pageIndex)
                                                .orElse(
                                                                FileOcrPage.builder()
                                                                                .file(file)
                                                                                .pageIndex(pageIndex)
                                                                                .build());

                                page.setOcrText("");
                                page.setOcrStatus(FileOcrPage.OcrStatus.FAILED);
                                fileOcrPageRepository.save(page);
                        }
                }

                List<FileOcrPage> storedPages = fileOcrPageRepository
                                .findByFileOrderByPageIndexAsc(file);

                List<PageOcrResultDTO> pages = storedPages.stream()
                                .map(page -> PageOcrResultDTO.builder()
                                                .pageIndex(page.getPageIndex())
                                                .text(page.getOcrText())
                                                .build())
                                .toList();

                List<Integer> failedPageIndexes = storedPages.stream()
                                .filter(page -> page.getOcrStatus() == FileOcrPage.OcrStatus.FAILED)
                                .map(FileOcrPage::getPageIndex)
                                .toList();

                String fullText = pages.stream()
                                .map(PageOcrResultDTO::getText)
                                .filter(text -> text != null && !text.isBlank())
                                .collect(Collectors.joining("\n\n"));

                if (fullText.isBlank()) {
                        file.setExtractionStatus(ExtractionStatus.FAILED);
                        file.setExtractionError(
                                        "Không nhận diện được nội dung OCR");
                } else {
                        String normalizedText = textNormalizer.normalize(fullText);

                        file.setRawText(fullText);
                        file.setNormalizedText(normalizedText);
                        file.setCharacterCount(normalizedText.length());
                        file.setWordCount(countWords(normalizedText));

                        if (isLastBatch && failedPageIndexes.isEmpty()) {
                                file.setExtractionStatus(
                                                ExtractionStatus.EXTRACTED);
                                file.setExtractionError(null);
                        } else {
                                file.setExtractionStatus(
                                                ExtractionStatus.PROCESSING);

                                if (!failedPageIndexes.isEmpty()) {
                                        file.setExtractionError(
                                                        "Một số trang OCR thất bại: "
                                                                        + failedPageIndexes);
                                }
                        }
                }

                fileUploadRepository.save(file);
                // Nếu là patch cuối cùng và không có trang OCR thất bại, tiến hành xử lý
                // template
                if (isLastBatch
                                && failedPageIndexes.isEmpty()
                                && !fullText.isBlank()) {

                        // Gọi dịch vụ xử lý template bất đồng bộ
                        templateProcessingService.submit(
                                        file.getFileId(),
                                        session.getSessionId(),
                                        currentUser.getUserId());
                }

                if (file.getRawText() != null) {
                        fileTextRedisRepository.save(
                                        file.getFileId(),
                                        file.getRawText());
                }

                return ScanOcrResponseDTO.builder()
                                .fileId(file.getFileId())
                                .pages(pages)
                                .failedPageIndexes(failedPageIndexes)
                                .fullText(fullText)
                                .build();
        }

        // Validate OCR image and page index before processing
        private void validateOcrImage(MultipartFile image) {
                String contentType = image.getContentType();

                if (!"image/png".equalsIgnoreCase(contentType)
                                && !"image/jpeg".equalsIgnoreCase(contentType)
                                && !"image/jpg".equalsIgnoreCase(contentType)) {
                        throw new IllegalArgumentException(
                                        "OCR chỉ hỗ trợ ảnh PNG hoặc JPEG");
                }
        }

        // Validate page index before processing
        private void validatePageIndex(Integer pageIndex) {
                if (pageIndex == null || pageIndex < 1) {
                        throw new IllegalArgumentException(
                                        "pageIndex phải bắt đầu từ 1");
                }
        }

        // Xử lý OCR và AI cho các ảnh đã quét từ file PDF.
        public ScanAiTextResponseDTO processScanAiText(
                        String rawFileId,
                        String sessionUuid,
                        List<MultipartFile> images,
                        List<Integer> pageIndexes,
                        boolean isLastBatch) throws IOException {

                log.debug(
                                "AI scan bắt đầu: fileId={}, sessionUuid={}, imageCount={}, "
                                                + "pageIndexCount={}, isLastBatch={}",
                                rawFileId,
                                sessionUuid,
                                images != null ? images.size() : null,
                                pageIndexes != null ? pageIndexes.size() : null,
                                isLastBatch);

                if (images == null
                                || pageIndexes == null
                                || images.isEmpty()
                                || images.size() > 5
                                || images.size() != pageIndexes.size()) {
                        throw new IllegalArgumentException(
                                        "Mỗi request phải gửi từ 1 đến 5 ảnh "
                                                        + "và pageIndexes tương ứng");
                }

                if (pageIndexes.stream().distinct().count() != pageIndexes.size()) {
                        throw new IllegalArgumentException(
                                        "pageIndexes không được trùng nhau");
                }

                log.debug(
                                "AI scan input hợp lệ: fileId={}, pages={}",
                                rawFileId,
                                pageIndexes);

                User currentUser = userRepository
                                .findByEmail(resolveCurrentEmail())
                                .orElseThrow(() -> new RuntimeException(
                                                "Tài khoản không tồn tại!"));

                ChatSession session = chatSessionRepository
                                .findBySessionUuidAndUser_UserId(
                                                sessionUuid,
                                                currentUser.getUserId())
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Session không tồn tại "
                                                                + "hoặc không thuộc user"));

                String fileId = normalizeFileId(rawFileId);

                FileMetadata file = fileUploadRepository
                                .findByFileIdAndUser_UserId(
                                                fileId,
                                                currentUser.getUserId())
                                .orElseThrow(() -> new NoSuchElementException(
                                                "File không tồn tại "
                                                                + "hoặc không thuộc user"));

                boolean attached = chatSessionFileRepository
                                .existsByChatSessionAndFile(
                                                session,
                                                file);

                if (!attached) {
                        throw new IllegalArgumentException(
                                        "File không thuộc session hiện tại");
                }

                // Lưu các ảnh OCR tạm thời vào bộ nhớ tạm thời để xử lý sau
                try {
                        for (int index = 0; index < images.size(); index++) {
                                MultipartFile image = images.get(index);
                                Integer pageIndex = pageIndexes.get(index);

                                validateOcrImage(image);
                                validatePageIndex(pageIndex);

                                scanAiTempStorage.save(fileId, pageIndex, image);
                        }
                } catch (IOException | RuntimeException exception) {
                        file.setExtractionStatus(ExtractionStatus.FAILED);
                        file.setExtractionError(
                                        "Không thể lưu ảnh scan: " + exception.getMessage());
                        fileUploadRepository.save(file);

                        throw exception;
                }

                if (!isLastBatch) {
                        file.setExtractionStatus(
                                        ExtractionStatus.PROCESSING);

                        fileUploadRepository.save(file);

                        return ScanAiTextResponseDTO.builder()
                                        .fileId(fileId)
                                        .completed(false)
                                        .extractionStatus(
                                                        ExtractionStatus.PROCESSING.name())
                                        .build();
                }
                // Nếu đây là batch cuối cùng, cập nhật trạng thái xử lý và gửi tác vụ xử lý AI
                file.setExtractionStatus(ExtractionStatus.PROCESSING);
                file.setExtractionError(null);
                fileUploadRepository.save(file);

                // Gửi tác vụ xử lý AI cho batch cuối cùng
                scanAiProcessingService.submit(
                                fileId,
                                session.getSessionId(),
                                currentUser.getUserId());

                return ScanAiTextResponseDTO.builder()
                                .fileId(fileId)
                                .completed(false)
                                .fullText(null)
                                .extractionStatus(
                                                ExtractionStatus.PROCESSING.name())
                                .build();

        }

        private FileMetadataResponseDTO toResponse(
                        FileMetadata file,
                        User user,
                        String fieldCode) {
                return FileMetadataResponseDTO.builder()
                                .fileId(file.getFileId())
                                .fileName(file.getFileName())
                                .fileUrl(file.getFileUrl())
                                .fileSize(file.getFileSize())
                                .category(
                                                file.getCategory() != null
                                                                ? file.getCategory().name()
                                                                : null)
                                .uploadedAt(file.getUploadedAt())
                                .uploadedBy(user.getFullName())
                                .extractionStatus(
                                                file.getExtractionStatus() != null
                                                                ? file.getExtractionStatus().name()
                                                                : null)
                                .extractionError(file.getExtractionError())
                                .rawText(
                                                shouldExposeRawText(
                                                                file.getCategory(),
                                                                fieldCode)
                                                                                ? file.getRawText()
                                                                                : null)
                                .build();
        }

        // Nếu file là file template thì hiển thị nội dung thô
        private boolean shouldExposeRawText(
                        FileMetadata.FileCategory category,
                        String fieldCode) {
                return category == FileMetadata.FileCategory.TEMPLATE;
        }

        // Lấy cấu hình upload dựa trên gói của người dùng hiện tại
        public UploadConfigResponseDTO getUploadConfig() {
                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication == null
                                || authentication.getPrincipal() == null
                                || "anonymousUser".equals(authentication.getPrincipal())) {
                        throw new RuntimeException(
                                        "Không tìm thấy thông tin đăng nhập");
                }

                String userId = authentication
                                .getPrincipal()
                                .toString();

                User currentUser = userRepository
                                .findById(userId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Tài khoản không tồn tại!"));

                AdminPackageConfig packageConfig = currentUser.getAdminPackageConfig() != null
                                ? currentUser.getAdminPackageConfig()
                                : adminService.getPackageConfig("FREE");
                log.info(
                                "Upload config package: userId={}, packageId={}, packageType={}, modelPackageLevel={}",
                                currentUser.getUserId(),
                                packageConfig.getId(),
                                packageConfig.getPackageType(),
                                packageConfig.getModelPackageLevel());
                if (packageConfig == null) {
                        throw new IllegalStateException(
                                        "Không tìm thấy cấu hình package");
                }

                long maxFileSizeMb = packageConfig.getMaxFileSizeMb() != null
                                ? packageConfig.getMaxFileSizeMb()
                                : 10L;

                int maxFiles = packageConfig.getMaxFilesPerCall() != null
                                ? packageConfig.getMaxFilesPerCall()
                                : 5;

                long storageQuotaMb = packageConfig.getStorageQuotaMb() != null
                                ? packageConfig.getStorageQuotaMb()
                                : getDefaultStorageQuotaMb(
                                                packageConfig.getPackageType());

                long storageQuota = storageQuotaMb * 1024L * 1024L;

                long usedStorage = Optional.ofNullable(
                                fileUploadRepository.sumFileSizeByUserId(
                                                currentUser.getUserId()))
                                .orElse(0L);

                long remainingStorage = Math.max(0L, storageQuota - usedStorage);

                long maxText = packageConfig.getMaxTextPerCall() != null
                                ? packageConfig.getMaxTextPerCall()
                                : 100000L;

                return UploadConfigResponseDTO.builder()
                                .packageType(packageConfig.getPackageType())
                                .maxFileSize(maxFileSizeMb * 1024L * 1024L)
                                .maxFiles(maxFiles)
                                .maxText(maxText)
                                .allowedExtensions(
                                                new ArrayList<>(
                                                                parseAllowedFileTypes(
                                                                                packageConfig.getAllowedFileTypes())))
                                .storageQuota(storageQuota)
                                .usedStorage(usedStorage)
                                .remainingStorage(remainingStorage)
                                .build();
        }

        // Lấy tên file từ URL lưu trữ
        private String extractStoredFileName(String fileUrl) {
                if (fileUrl == null || fileUrl.isBlank()) {
                        return null;
                }

                int lastSlashIndex = fileUrl.lastIndexOf('/');

                return lastSlashIndex >= 0
                                ? fileUrl.substring(lastSlashIndex + 1)
                                : fileUrl;
        }

        // Lấy trạng thái quét AI của một file dựa trên fileId và userId
        public ScanAiTextResponseDTO getScanStatus(
                        String fileId,
                        String userId) {

                FileMetadata file = fileUploadRepository
                                .findByFileIdAndUser_UserId(fileId, userId)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "File không tồn tại"));

                boolean completed = file.getExtractionStatus() == ExtractionStatus.EXTRACTED
                                || file.getExtractionStatus() == ExtractionStatus.FAILED;

                return ScanAiTextResponseDTO.builder()
                                .fileId(file.getFileId())
                                .completed(completed)
                                .fullText(file.getRawText())
                                .extractionStatus(file.getExtractionStatus().name())
                                .build();
        }
}