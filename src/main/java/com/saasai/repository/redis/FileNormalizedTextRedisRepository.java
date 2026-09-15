package com.saasai.repository.redis;

import java.util.Optional;
import java.util.UUID;

public interface FileNormalizedTextRedisRepository {
     
    // get raw text
    // delete raw text
    //file:raw-text:{fileId}

    // @interface RedisKey {
    //     String FILE_RAW_TEXT_PREFIX = "file:raw-text:";
    // }
    // @interface RedisTTL {
    //     long FILE_RAW_TEXT_TTL = 60 * 60 ; // 1 hour in seconds
    // }

    // @interface RedisHash {
    //     String FILE_RAW_TEXT_HASH = "file:raw-text";
    // }

    // @interface RedisField {
    //     String RAW_TEXT_FIELD = "rawText";
    // }

    void save(String fileId, String normalizedText);

    Optional<String> find(String fileId);

    void delete(String fileId);

}
