package com.saasai.repository.redis;

import com.saasai.dto.DraftStateDTO;

import java.util.Optional;

public interface DraftStateRedisRepository {
    void save(DraftStateDTO draftState);
    //Optional<DraftStateDTO> find(Integer sessionId, String userId);
    Optional<DraftStateDTO> find(String sessionUuid, String userId);
    void delete(String sessionUuid, String userId);

}