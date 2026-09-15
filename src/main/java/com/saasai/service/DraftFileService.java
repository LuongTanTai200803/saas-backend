package com.saasai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.dto.DocumentFormDTO;
import com.saasai.dto.DraftStateDTO;
import com.saasai.entity.ChatSession;
import com.saasai.entity.ChatSession.SessionStatus;
import com.saasai.entity.ChatSessionFile;
import com.saasai.repository.ChatSessionRepository;
import com.saasai.repository.redis.DraftStateRedisRepository;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class DraftFileService {

    private final DraftStateRedisRepository draftRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ObjectMapper objectMapper;
  

    public DraftFileService(
            DraftStateRedisRepository draftRepository,
            ChatSessionRepository chatSessionRepository,
            ObjectMapper objectMapper
            
    ) {
        this.draftRepository = draftRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.objectMapper = objectMapper;
    }

    public DraftStateDTO saveDraftState(
        String draftId,
        String userId,
        String editorText,
        DocumentFormDTO formData,
        String fieldCode
    ) {
        String normalizedDraftId = requireText(draftId, "draftId");
        String normalizedUserId = requireText(userId, "userId");

        ChatSession session =
                chatSessionRepository
                        .findBySessionUuidAndUser_UserId(
                                draftId,
                                normalizedUserId
                        )
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Session không tồn tại hoặc không thuộc user"
                                )
                        );

        DraftStateDTO state = new DraftStateDTO();

        state.setSessionUuid(normalizedDraftId);
        state.setUserId(normalizedUserId);

        /*
        * Ưu tiên FormData.
        *
        * FormData chính là nguồn dữ liệu mà AI sẽ dùng.
        */
        if (formData != null) {
            try {
                state.setWizardStateJson(
                        objectMapper.writeValueAsString(formData)
                );
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(
                        "Không thể serialize formData thành JSON",
                        e
                );
            }
        } else {
            state.setWizardStateJson(
                    editorText == null ? "" : editorText
            );
        }
        state.setEditorText(editorText);
        state.setFieldCode(fieldCode);
        state.setUpdatedAt(LocalDateTime.now());
        state.setStatus(
        session.getStatus() != null
                ? session.getStatus().name()
                : "DRAFT"
        );

        // Redis
        draftRepository.save(state);

        // DB mirror
        session.setWizardStateJson(state.getWizardStateJson());
        session.setStatus(
                mapDraftStatusToSessionStatus(state.getStatus())
        );
        
        chatSessionRepository.save(session);

        return state;
    }

    // Load the draft state from Redis. If not found, load from database.
    public DraftStateDTO loadDraftState(
        String sessionUuid,
        String userId
    ) {
        
        ChatSession session = chatSessionRepository
                .findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Session không tồn tại hoặc không thuộc user"
                        )
                );

        Integer sessionId = session.getSessionId();

        if (sessionId == null) {
            throw new IllegalArgumentException(
                    "sessionId không được để trống"
            );
        }

        String normalizedUserId =
                requireText(userId, "userId");

        
        return draftRepository.find(
                    sessionUuid,
                    normalizedUserId
            )
            .or(() ->
                loadDraftStateFromDatabase(
                    sessionUuid,
                    normalizedUserId
                )
            )
            .orElseThrow(() ->
                new NoSuchElementException(
                    "Draft không tồn tại hoặc đã hết hạn"
                )
            );
    }

    
    public String finalizeDraftAndBuildPrompt(
            String sessionUuid,
            String userId
    ) {
        DraftStateDTO draft =
                loadDraftState(sessionUuid, userId);

        validateSessionOwnership(
                draft.getSessionUuid(),
                userId
        );

        String wizardText = draft.getWizardStateJson();

        if (wizardText == null || wizardText.isBlank()) {
            throw new IllegalArgumentException(
                    "Draft không có nội dung để hoàn tất"
            );
        }

        draft.setStatus("FINALIZED");
        draft.setUpdatedAt(LocalDateTime.now());

        draftRepository.save(draft);

        return wizardText;
    }

    private Optional<DraftStateDTO> loadDraftStateFromDatabase(
            String sessionUuid,
            String userId
    ) {

        // Load from database
        return chatSessionRepository
                .findBySessionUuid(sessionUuid)
                .map(session -> {
                    DraftStateDTO state = new DraftStateDTO();
                    state.setSessionUuid(sessionUuid); // Assuming draftId is the same as sessionId for this example
                    state.setUserId(userId);
    
                    state.setEditorText(
                            session.getEditorContent() == null
                                    ? ""
                                    : session.getEditorContent()
                    );
                    state.setStatus(
                            session.getStatus() == null
                                    ? "DRAFT"
                                    : session.getStatus().name()
                    );
                    state.setWizardStateJson(session.getWizardStateJson());
                    state.setUpdatedAt(LocalDateTime.now());
                    return state;
                });
    }

    // Xoá bản ghi 
    public void deleteDraft(String draftId, String userId) {
        draftRepository.delete(
                requireText(draftId, "draftId"),
                requireText(userId, "userId")
        );
    }

    private void validateSessionOwnership(String sessionUuid, String userId) {
        if (sessionUuid == null) {
            throw new IllegalArgumentException("sessionId không được null");
        }

        chatSessionRepository.findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Session không tồn tại hoặc không thuộc user"
                ));
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

            String normalized = fileId.trim();

            if (normalized.startsWith("file_")) {
                normalized = normalized.substring("file_".length());
            }

            uniqueIds.add(normalized);
        }

        return new ArrayList<>(uniqueIds);
    }

    private SessionStatus mapDraftStatusToSessionStatus(String status) {
        if (status == null || status.isBlank()) {
            return SessionStatus.DRAFT;
        }

        return switch (status.trim().toUpperCase()) {
            case "EDITING" -> SessionStatus.EDITING;
            case "FINALIZED" -> SessionStatus.COMPLETED;
            case "ARCHIVED" -> SessionStatus.ARCHIVED;
            case "DRAFT" -> SessionStatus.DRAFT;
            default -> SessionStatus.DRAFT;
        };
    }
    
    // 
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }

        return value.trim();
    }

    // Lấy nội dung editorText của draft dựa trên sessionUuid và userId. Nếu không tìm thấy trong Redis, sẽ fallback sang DB.
    public String getFormDataBySession(
        String sessionUuid,
        String userId
    ) {
        String normalizedUserId = requireText(userId, "userId");

        // Redis
        Optional<DraftStateDTO> cached =
                draftRepository.find(sessionUuid, normalizedUserId);

                
        if (cached.isPresent()) {
        String wizardStateJson = cached.get().getWizardStateJson();

        if (wizardStateJson != null && !wizardStateJson.isBlank()) {
            return wizardStateJson;
        }
    }

        
        // DB fallback
        ChatSession session = chatSessionRepository
                .findBySessionUuidAndUser_UserId(sessionUuid, normalizedUserId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Session không tồn tại"
                ));

        String wizardText = session.getWizardStateJson();

        // Có thể re-cache Redis
        DraftStateDTO state = new DraftStateDTO();
        state.setSessionUuid(sessionUuid);
        state.setUserId(normalizedUserId);
        state.setWizardStateJson(
                wizardText == null ? "" : wizardText
        );

        state.setStatus("EDITING");
        state.setUpdatedAt(LocalDateTime.now());

        draftRepository.save(state);

        return wizardText == null ? "" : wizardText;
    }

    public String getEditorTextBySession(
        String sessionUuid,
        String userId
    ) {
        String normalizedUserId = requireText(userId, "userId");

        // Redis
        Optional<DraftStateDTO> cached =
                draftRepository.find(sessionUuid, normalizedUserId);

        if (cached.isPresent()) {

            String editorText = cached.get().getEditorText();

            // Redis có record nhưng editorText null/rỗng
            // → phải tiếp tục kiểm tra DB
            if (editorText != null && !editorText.isBlank()) {
                return editorText;
            }
        }

        
        // DB fallback
        ChatSession session = chatSessionRepository
                .findBySessionUuidAndUser_UserId(sessionUuid, normalizedUserId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Session không tồn tại"
                ));

        String editorText = session.getEditorContent();

        if (editorText == null || editorText.isBlank()) {
            return "";
        }

        // Có thể re-cache Redis
        DraftStateDTO state = new DraftStateDTO();
        state.setSessionUuid(sessionUuid);
        state.setUserId(normalizedUserId);
        state.setEditorText(
                editorText == null ? "" : editorText
        );

        state.setStatus("EDITING");
        state.setUpdatedAt(LocalDateTime.now());

        draftRepository.save(state);

        return editorText == null ? "" : editorText;
    }
}