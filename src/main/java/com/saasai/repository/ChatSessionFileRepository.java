package com.saasai.repository;

import com.saasai.entity.ChatSession;
import com.saasai.entity.ChatSessionFile;
import com.saasai.entity.ChatSessionFile.PromptFieldCode;
import com.saasai.entity.FileMetadata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatSessionFileRepository
        extends JpaRepository<ChatSessionFile, Long> {

    List<ChatSessionFile> findByChatSession(ChatSession chatSession);

    List<ChatSessionFile> findByChatSessionOrderByFieldCodeAscSortOrderAsc(
            ChatSession chatSession
    );

    List<ChatSessionFile> findByChatSessionAndFieldCodeOrderBySortOrderAsc(
        ChatSession chatSession,
        ChatSessionFile.PromptFieldCode fieldCode
        );

    void deleteByChatSession(ChatSession chatSession);

    @Query("""
    SELECT MAX(c.sortOrder)
    FROM ChatSessionFile c
    WHERE c.chatSession = :session
      AND c.fieldCode = :fieldCode
    """)
    Optional<Integer> findMaxSortOrderBySessionAndField(
            @Param("session") ChatSession session,
            @Param("fieldCode") ChatSessionFile.PromptFieldCode fieldCode
    );

    boolean existsByChatSessionAndFileAndFieldCode(
                ChatSession chatSession,
                FileMetadata file,
                ChatSessionFile.PromptFieldCode fieldCode
        );

        @Query("""
        SELECT file
        FROM ChatSessionFile sessionFile
        JOIN sessionFile.file file
        WHERE sessionFile.chatSession.sessionId = :sessionId
        AND file.user.userId = :userId
        AND file.contentHash = :contentHash
        """)
        Optional<FileMetadata> findBySessionAndUserAndContentHash(
                @Param("sessionId") Integer sessionId,
                @Param("userId") String userId,
                @Param("contentHash") String contentHash
        );

        boolean existsByChatSessionAndFile(
                ChatSession chatSession,
                FileMetadata file
        );
}