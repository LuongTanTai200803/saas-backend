package com.saasai.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.saasai.entity.Assistant;
import com.saasai.entity.ChatSession;
import com.saasai.entity.User;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Integer> {
    Page<ChatSession> findByUser_UserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);

    Optional<ChatSession> findBySessionIdAndUser_UserId(Integer sessionId, String userId);

    Optional<ChatSession> findBySessionUuidAndUser_UserId(String sessionUuid, String userId);

    List<ChatSession> findByUser_UserId(String userId);

    List<ChatSession> findByUser_UserIdAndAssistant_AssistantId(
            String userId,
            Integer assistantId);

    Optional<ChatSession> findBySessionIdAndUser_Email(Integer sessionId, String email);

    Optional<ChatSession> findBySessionId(Integer sessionId);

    Optional<ChatSession> findBySessionUuid(String sessionUuid);

    @Modifying
    @Transactional
    @Query("""
            UPDATE ChatSession s
            SET s.templateDefinitionJson = :templateJson,
                s.updatedAt = CURRENT_TIMESTAMP
            WHERE s.sessionId = :sessionId
            """)
    int updateTemplateDefinition(
            @Param("sessionId") Integer sessionId,
            @Param("templateJson") String templateJson);
}
