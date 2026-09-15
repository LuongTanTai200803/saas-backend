package com.saasai.service;

import com.saasai.entity.ChatSession;
import com.saasai.entity.ChatSessionFile;
import com.saasai.entity.FileMetadata;
import com.saasai.repository.ChatSessionFileRepository;
import com.saasai.repository.ChatSessionRepository;
import com.saasai.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSessionFileService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatSessionFileRepository chatSessionFileRepository;
    private final FileMetadataRepository fileMetadataRepository;

    @Transactional
    public void attachFileToSession(
            Integer sessionId,
            String userId,
            String fileId,
            String fieldCode
    ) {
        if (sessionId == null || userId == null || userId.isBlank()) {
            return;
        }
        System.out.println("Attaching file to session: sessionId=" + sessionId + ", userId=" + userId + ", fileId=" + fileId + ", fieldCode=" + fieldCode);
        
        ChatSession session = chatSessionRepository
                .findBySessionIdAndUser_UserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Session không tồn tại hoặc không thuộc user"
                ));

        ChatSessionFile.PromptFieldCode resolvedField =
                fieldCode != null
                        ? ChatSessionFile.PromptFieldCode.valueOf(fieldCode)
                        : ChatSessionFile.PromptFieldCode.REFERENCE;

        String normalizedFileId = normalizeFileId(fileId);

        FileMetadata fileMetadata = fileMetadataRepository
                .findByFileIdAndUser_UserId(normalizedFileId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy file hoặc bạn không có quyền truy cập: "
                                + fileId
                ));

        int nextSortOrder = chatSessionFileRepository
                .findMaxSortOrderBySessionAndField(
                        session,
                        resolvedField
                )
                .orElse(-1) + 1;

        ChatSessionFile sessionFile = ChatSessionFile.builder()
                .chatSession(session)
                .file(fileMetadata)
                .fieldCode(resolvedField)
                .sortOrder(nextSortOrder)
                .build();

        chatSessionFileRepository.save(sessionFile);
    }

    private List<String> normalizeFileIds(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> uniqueIds = new LinkedHashSet<>();
        for (String fileId : fileIds) {
            if (fileId == null || fileId.isBlank()) {
                continue;
            }
            uniqueIds.add(fileId.trim());
        }
        return new ArrayList<>(uniqueIds);
    }

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

    public List<FileMetadata> getFilesBySession(ChatSession session) {
        return chatSessionFileRepository
                .findByChatSession(session)
                .stream()
                .filter(chatSessionFile -> chatSessionFile.getId() != null)
                .map(ChatSessionFile::getFile)
                .filter(fileMetadata -> fileMetadata.getFileId() != null)
                .toList();
    }

    public List<String> getFileIdsBySession(ChatSession session) {
        return getFilesBySession(session).stream()
                .map(FileMetadata::getFileId)
                .toList();
        }
}