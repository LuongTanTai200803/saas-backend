package com.saasai.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.saasai.entity.FileMetadata;
import com.saasai.entity.FileMetadata.ExtractionStatus;
import com.saasai.normalizer.TextNormalizer;
import com.saasai.repository.FileMetadataRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ScanAiProcessingService {
    private static final Logger log = LoggerFactory.getLogger(ScanAiProcessingService.class);

    private final FileMetadataRepository fileMetadataRepository;
    private final ScanAiTempStorage scanAiTempStorage;
    private final AiVisionService aiVisionService;
    private final TextNormalizer textNormalizer;
    private final TemplateProcessingService templateProcessingService;

    @Async("scanTaskExecutor")
    public void submit(String fileId, Integer sessionId, String userId) {
        try {
            FileMetadata file = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Không tìm thấy file: " + fileId));

            List<Path> imagePaths = scanAiTempStorage.listImagesOrderByPageIndex(fileId);

            String accumulatedText = "";

            for (int start = 0; start < imagePaths.size(); start += 2) {
                int end = Math.min(start + 2, imagePaths.size());

                String chunkText = aiVisionService.extractText(
                        imagePaths.subList(start, end));

                if (chunkText == null || chunkText.isBlank()) {
                    throw new IllegalStateException(
                            "AI không trả về nội dung");
                }

                accumulatedText = accumulatedText.isBlank()
                        ? chunkText
                        : accumulatedText + "\n\n" + chunkText;

                String normalizedText = textNormalizer.normalize(accumulatedText);

                file.setRawText(accumulatedText);
                file.setNormalizedText(normalizedText);
                file.setCharacterCount(normalizedText.length());
                file.setWordCount(countWords(normalizedText));
                file.setExtractionStatus(ExtractionStatus.PROCESSING);
                file.setExtractionError(null);

                fileMetadataRepository.save(file);
            }

            file.setExtractionStatus(ExtractionStatus.EXTRACTED);
            fileMetadataRepository.save(file);

            try {
                templateProcessingService.submit(
                        fileId,
                        sessionId,
                        userId);
            } catch (Exception templateException) {
                // OCR đã hoàn tất, lỗi template không được chuyển file về FAILED
                log.error(
                        "Không thể gửi tác vụ template: fileId={}, sessionId={}",
                        fileId,
                        sessionId,
                        templateException);
            }

            log.info("Scan AI hoàn tất: fileId={}", fileId);

        } catch (Exception exception) {
            try {
                fileMetadataRepository.findById(fileId).ifPresent(file -> {
                    file.setExtractionStatus(ExtractionStatus.FAILED);
                    file.setExtractionError(exception.getMessage());
                    fileMetadataRepository.save(file);
                });
            } catch (Exception statusException) {
                log.error(
                        "Không thể cập nhật FAILED: fileId={}",
                        fileId,
                        statusException);
            }

            log.error("Scan AI thất bại: fileId={}", fileId, exception);

        } finally {
            try {
                scanAiTempStorage.delete(fileId);
            } catch (IOException exception) {
                log.warn(
                        "Không thể xóa ảnh tạm sau khi xử lý: fileId={}",
                        fileId,
                        exception);
            }
        }
    }

    private int countWords(String text) {
        return text == null || text.isBlank()
                ? 0
                : text.trim().split("\\s+").length;
    }
}