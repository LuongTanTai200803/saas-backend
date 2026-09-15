package com.saasai.service;

import com.saasai.entity.FileMetadata;
import com.saasai.repository.FileMetadataRepository;
import com.saasai.repository.redis.FileNormalizedTextRedisRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FileTextService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileNormalizedTextRedisRepository fileTextRedisRepository;

    public FileTextService(
            FileMetadataRepository fileMetadataRepository,
            FileNormalizedTextRedisRepository fileTextRedisRepository
    ) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileTextRedisRepository = fileTextRedisRepository;
    }

    // Lấy normalizedText theo flow Redis-first, nếu không có thì lấy từ DB và cache vào Redis
    public String getNormalizedText(String rawFileId, String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId không được null");
        }

        String fileId = normalizeFileId(rawFileId);

        return fileTextRedisRepository.find(fileId)
                .orElseGet(() -> loadFromDatabaseAndCache(fileId, userId));
    }
   
    // Xóa normalizedText khỏi Redis cache, ví dụ khi file bị xóa hoặc cập nhật
    public void evictNormalizedText(String rawFileId) {
        fileTextRedisRepository.delete(
                normalizeFileId(rawFileId)
        );
    }

    // Lấy normalizedText từ DB và cache vào Redis
    private String loadFromDatabaseAndCache(
            String fileId,
            String userId
    ) {
        FileMetadata fileMetadata = fileMetadataRepository
                .findByFileIdAndUser_UserId(fileId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy file hoặc bạn không có quyền truy cập"
                ));

        String normalizedText = fileMetadata.getNormalizedText();

        if (normalizedText == null || normalizedText.isBlank()) {
            throw new IllegalStateException(
                    "File chưa có normalized text: file_" + fileId
            );
        }

        fileTextRedisRepository.save(fileId, normalizedText);

        return normalizedText;
    }

    // Chuẩn hóa fileId: loại bỏ tiền tố "file_" nếu có, và kiểm tra không null/blank
    private String normalizeFileId(String rawFileId) {
        if (rawFileId == null || rawFileId.isBlank()) {
            throw new IllegalArgumentException("fileId không được để trống");
        }

        String normalized = rawFileId.trim();

        if (normalized.startsWith("file_")) {
            normalized = normalized.substring("file_".length());
        }

        return normalized;
    }
}