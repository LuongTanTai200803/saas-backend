package com.saasai.service;

import com.saasai.entity.FileMetadata;
import com.saasai.repository.FileMetadataRepository;
import com.saasai.repository.redis.FileNormalizedTextRedisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileTextServiceTest {

@Mock
private FileNormalizedTextRedisRepository fileTextRedisRepository;

@Mock
private FileMetadataRepository fileMetadataRepository;

@InjectMocks
private FileTextService fileTextService;

@Test
void getNormalizedText_shouldReturnFromRedis_whenCacheHit() {
    String fileId = UUID.randomUUID().toString();
    String userId = "user-uuid-1";
    String cachedText = "normalized-from-redis";

    when(fileTextRedisRepository.find(fileId)).thenReturn(Optional.of(cachedText));

    String result = fileTextService.getNormalizedText(fileId, userId);

    assertEquals(cachedText, result);
    verify(fileMetadataRepository, never()).findByFileIdAndUser_UserId(fileId, userId);
    verify(fileTextRedisRepository, never()).save(fileId, cachedText);
}

@Test
void getNormalizedText_shouldFallbackToDbAndCache_whenCacheMiss() {
    String fileId = UUID.randomUUID().toString();
    String userId = "user-uuid-1";
    String dbText = "normalized-from-db";

    FileMetadata metadata = FileMetadata.builder()
            .fileId(fileId)
            .normalizedText(dbText)
            .build();

    when(fileTextRedisRepository.find(fileId)).thenReturn(Optional.empty());
    when(fileMetadataRepository.findByFileIdAndUser_UserId(fileId, userId))
            .thenReturn(Optional.of(metadata));

    String result = fileTextService.getNormalizedText(fileId, userId);

    assertEquals(dbText, result);
    verify(fileMetadataRepository).findByFileIdAndUser_UserId(fileId, userId);
    verify(fileTextRedisRepository).save(fileId, dbText);
}

@Test
void getNormalizedText_shouldThrow_whenNotFoundInRedisAndDb() {
    String fileId = UUID.randomUUID().toString();
    String userId = "user-uuid-1";

    when(fileTextRedisRepository.find(fileId)).thenReturn(Optional.empty());
    when(fileMetadataRepository.findByFileIdAndUser_UserId(fileId, userId))
            .thenReturn(Optional.empty());

    RuntimeException ex = assertThrows(
            RuntimeException.class,
            () -> fileTextService.getNormalizedText(fileId, userId)
    );

    assertEquals("File không tồn tại hoặc không thuộc về người dùng hiện tại!", ex.getMessage());
    verify(fileMetadataRepository).findByFileIdAndUser_UserId(fileId, userId);
}

@Test
void getNormalizedText_shouldStripFilePrefix_andStillWork() {
    String rawFileId = "file_" + UUID.randomUUID();
    String fileId = rawFileId.substring(5);
    String userId = "user-uuid-1";
    String dbText = "normalized-from-db";

    FileMetadata metadata = FileMetadata.builder()
            .fileId(fileId)
            .normalizedText(dbText)
            .build();

    when(fileTextRedisRepository.find(fileId)).thenReturn(Optional.empty());
    when(fileMetadataRepository.findByFileIdAndUser_UserId(fileId, userId))
            .thenReturn(Optional.of(metadata));

    String result = fileTextService.getNormalizedText(rawFileId, userId);

    assertEquals(dbText, result);
    verify(fileTextRedisRepository).find(fileId);
    verify(fileTextRedisRepository).save(fileId, dbText);
}

@Test
void getNormalizedText_shouldThrow_whenFileIdInvalidUuid() {
    String badFileId = "file_not-a-uuid";
    String userId = "user-uuid-1";

    IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> fileTextService.getNormalizedText(badFileId, userId)
    );

    assertEquals("fileId không hợp lệ: " + badFileId, ex.getMessage());
    verify(fileTextRedisRepository, never()).find(org.mockito.ArgumentMatchers.anyString());
    verify(fileMetadataRepository, never()).findByFileIdAndUser_UserId(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
}
}