package com.saasai.repository.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

import java.time.Duration;

@Repository
public class FileNormalizedTextRedisRepositoryImpl implements FileNormalizedTextRedisRepository {

    private static final String KEY_PREFIX = "file:normalized-text:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Logger logger = LoggerFactory.getLogger(FileNormalizedTextRedisRepositoryImpl.class);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled;

    public FileNormalizedTextRedisRepositoryImpl(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper
    ) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String fileId, String normalizedText) {
        validateFileId(fileId);

        if (normalizedText == null) {
            throw new IllegalArgumentException("normalizedText must not be null");
        }

        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null || !redisEnabled) {
            return;
        }
        try {
            redis.opsForValue().set(
                    buildKey(fileId),
                    normalizedText,
                    DEFAULT_TTL
            );
        } catch (Exception e) {
            logger.warn("Redis error while saving normalized text for fileId={}", fileId, e);
        }
    }

    @Override
    public Optional<String> find(String fileId) {
        validateFileId(fileId);

        // Retrieve the normalized text from Redis
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null || !redisEnabled) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(redis.opsForValue().get(buildKey(fileId)
            ));
        } catch (Exception e) {
            logger.warn("Redis error while finding normalized text for fileId={}", fileId, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String fileId) {
        validateFileId(fileId);

        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null || !redisEnabled) return;
        try {
            redis.delete(
                    buildKey(fileId)
            );
        } catch (Exception e) {
            logger.warn("Redis error while deleting normalized text for fileId={}", fileId, e);
        }
    }

    private String buildKey(String fileId) {
        return KEY_PREFIX + fileId;
    }

    private void validateFileId(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("fileId must not be blank");
        }
    }
}