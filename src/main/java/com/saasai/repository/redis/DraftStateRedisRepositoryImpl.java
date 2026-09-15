package com.saasai.repository.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.dto.DraftStateDTO;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class DraftStateRedisRepositoryImpl implements DraftStateRedisRepository {

    private static final String KEY_PATTERN = "draft:user:%s:%s";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(DraftStateRedisRepositoryImpl.class);

    private final Duration ttl;

    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled;

    public DraftStateRedisRepositoryImpl(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper,
            @Value("${app.redis.draft-ttl-hours:72}") long ttlHours
    ) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofHours(ttlHours);
    }

    @Override
    public void save(DraftStateDTO draftState) {
        validate(draftState.getSessionUuid(), draftState.getUserId());

        if (!redisEnabled) {
            return;
        }

        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(draftState);
            try {
                redis.opsForValue().set(
                        buildKey(draftState.getSessionUuid(), draftState.getUserId()),
                        json,
                        ttl
                );
            } catch (Exception redisEx) {
                logger.warn("Redis error while saving draft (fallback no-op) for session={}, user={}",
                        draftState.getSessionUuid(), draftState.getUserId(), redisEx);
                // fallback: no-op (DB is already the source of truth elsewhere)
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể serialize draft state", exception);
        }
    }

    @Override
    public Optional<DraftStateDTO> find(String sesisonUuid, String userId) {
        validate(sesisonUuid, userId);

        if (!redisEnabled) {
            return Optional.empty();
        }

        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return Optional.empty();
        }

        try {
            String json = redis.opsForValue().get(buildKey(sesisonUuid, userId));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(objectMapper.readValue(json, DraftStateDTO.class));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Không thể deserialize draft state", exception);
            }
        } catch (Exception e) {
            logger.warn("Redis error while reading draft for session={}, user={} — falling back", sesisonUuid, userId, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String sessionUuid, String userId) {
        validate(sessionUuid, userId);

        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null || !redisEnabled) return;
        try {
            redis.delete(buildKey(sessionUuid, userId));
        } catch (Exception e) {
            logger.warn("Redis error while deleting draft for session={}, user={}", sessionUuid, userId, e);
        }
    }

    private String buildKey(String sessionUuid, String userId) {
        return String.format(KEY_PATTERN, userId.trim(), sessionUuid.toString());
    }

    private void validate(String sessionUuid, String userId) {
        if (sessionUuid == null) {
            throw new IllegalArgumentException("sessionUuid không được để trống");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId không được để trống");
        }
    }
}