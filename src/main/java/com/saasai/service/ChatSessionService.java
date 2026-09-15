package com.saasai.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.saasai.dto.ChatMessageDTO;
import com.saasai.dto.ChatSessionCreateRequestDTO;
import com.saasai.dto.ChatSessionDTO;
import com.saasai.dto.ChatSessionWorkspaceDTO;
import com.saasai.dto.DraftStateDTO;
import com.saasai.entity.Assistant;
import com.saasai.entity.ChatSession;
import com.saasai.entity.ChatSession.SessionStatus;
import com.saasai.entity.User;
import com.saasai.repository.AssistantRepository;
import com.saasai.repository.ChatSessionRepository;
import com.saasai.repository.UserRepository;
import com.saasai.repository.redis.DraftStateRedisRepository;

import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSessionService {
    private static final Logger logger = LoggerFactory.getLogger(ChatSessionService.class);

    private static final String DEFAULT_SESSION_NAME = "Cuộc trò chuyện mới";
    private static final String DRAFT_KEY_PATTERN = "chat:session:%s:%d:update";

    private final ChatSessionRepository chatSessionRepository;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final UserRepository userRepository;
    private final AssistantRepository assistantRepository;
    private final DraftStateRedisRepository draftRepository;
    private final ObjectMapper objectMapper;

    // helper: get redis if available
    private StringRedisTemplate getRedis() {
        if (!redisEnabled) return null;
        return redisTemplateProvider.getIfAvailable();
    }

    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled;


    private void safeSet(String key, String value, Runnable dbFallback) {
        StringRedisTemplate redis = getRedis();
        if (redis == null) {
            dbFallback.run();
            return;
        }
        try {
            redis.opsForValue().set(key, value, Duration.ofDays(3));
        } catch (Exception e) {
            logger.warn("Redis unavailable while setting key {}, falling back to DB", key, e);
            dbFallback.run();
        }
    }

    private String safeGet(String key, Supplier<String> dbFallback) {
        StringRedisTemplate redis = getRedis();
        if (redis == null) {
            return dbFallback.get();
        }
        try {
            String v = redis.opsForValue().get(key);
            return (v != null) ? v : dbFallback.get();
        } catch (Exception e) {
            logger.warn("Redis unavailable while getting key {}, falling back to DB", key, e);
            return dbFallback.get();
        }
    }

    private void safeDelete(String key) {
        StringRedisTemplate redis = getRedis();
        if (redis == null) return;
        try {
            redis.delete(key);
        } catch (Exception e) {
            logger.warn("Redis unavailable while deleting key {}", key, e);
        }
    }

    public ChatSessionDTO createSession(String email, ChatSessionCreateRequestDTO request) {
        String currentEmail = resolveCurrentEmail(email);

        Assistant assistant = assistantRepository.findById(request.getAssistantId())
                .orElseThrow(() -> new RuntimeException("Trợ lý không tồn tại!"));

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        String normalizedSessionName = request.getSessionName() == null || request.getSessionName().trim().isEmpty()
                ? DEFAULT_SESSION_NAME
                : request.getSessionName().trim();

        ChatSession session = ChatSession.builder()
                .user(user)
                .assistant(assistant)
                .sessionName(normalizedSessionName)
                .editorContent("")
                .htmlContent("")
                .build();

        ChatSession saved = chatSessionRepository.save(session);

        return ChatSessionDTO.builder()
                .sessionUuid(saved.getSessionUuid())
                .tagId(saved.getTagId())
                .sessionName(saved.getSessionName())
                .currentEditorContent(saved.getEditorContent())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public ChatSessionWorkspaceDTO getWorkspace(String userId, String sessionUuid) {
        ChatSession session = chatSessionRepository
                .findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        String editorText = session.getEditorContent();
        if (editorText == null) editorText = "";

        return ChatSessionWorkspaceDTO.builder()
                .sessionUuid(sessionUuid)
                .status(session.getStatus() == null ? ChatSession.SessionStatus.DRAFT.name() : session.getStatus().name())
                .editorText(editorText)
                .messages(parseChatHistoryJson(session.getChatHistoryJson()))
                .build();
    }

    private List<ChatMessageDTO> parseChatHistoryJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ChatMessageDTO>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot parse chatHistoryJson", e);
        }
    }

    public void updateEditorContent(String sessionUuid, String email, String htmlContent) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NoSuchElementException("User not found"));

        String key = String.format(DRAFT_KEY_PATTERN, user.getUserId(), sessionUuid);

        // fallback: persist to DB by updating session.htmlContent
        Runnable dbFallback = () -> {
            try {
                ChatSession session = chatSessionRepository
                        .findBySessionUuidAndUser_UserId(sessionUuid, user.getUserId())
                        .orElseThrow(() -> new NoSuchElementException("Session not found"));
                session.setHtmlContent(htmlContent);
                chatSessionRepository.save(session);
            } catch (Exception ex) {
                logger.warn("Failed to fallback-save editor content to DB", ex);
            }
        };

        safeSet(key, htmlContent, dbFallback);
    }

    private String resolveCurrentEmail(String fallbackEmail) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof String details && !details.isBlank()) {
            return details;
        }
        return fallbackEmail;
    }

    public ChatSession getSession(String sessionUuid, String userId) {
        return chatSessionRepository.findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    public void saveWizardState(String sessionUuid, String userId, String wizardStateJson) {
        ChatSession session = getSession(sessionUuid, userId);
        session.setWizardStateJson(wizardStateJson);
        chatSessionRepository.save(session);
    }

    public void saveChatHistory(String sessionUuid, String userId, String chatHistoryJson) {
        ChatSession session = getSession(sessionUuid, userId);
        session.setChatHistoryJson(chatHistoryJson);
        chatSessionRepository.save(session);
    }

    public void saveDraft(String userId, String sessionUuid, String content) {
        ChatSession session = chatSessionRepository.findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        String key = String.format(DRAFT_KEY_PATTERN, userId, sessionUuid);

        Runnable dbFallback = () -> {
            session.setHtmlContent(content);
            chatSessionRepository.save(session);
        };

        safeSet(key, content, dbFallback);
    }

    public String loadEditorContent(String userId, String sessionUuid) {
        ChatSession session = chatSessionRepository.findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        String key = String.format(DRAFT_KEY_PATTERN, userId, sessionUuid);
        return safeGet(key, () -> {
            String dbContent = session.getHtmlContent();
            if (dbContent == null || dbContent.isBlank()) dbContent = session.getEditorContent();
            return dbContent != null ? dbContent : "";
        });
    }

    public byte[] exportCurrentDraft(String userId, String sessionUuid) {
        ChatSession session = chatSessionRepository.findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiên chat tương ứng!"));

        String key = String.format(DRAFT_KEY_PATTERN, userId, sessionUuid);
        String currentContent = safeGet(key, () -> {
            String dbContent = session.getHtmlContent();
            if (dbContent == null || dbContent.isBlank()) dbContent = session.getEditorContent();
            return dbContent != null ? dbContent : "";
        });

        return currentContent.getBytes(StandardCharsets.UTF_8);
    }

    public String finalizeAndSendToAi(String userId, String sessionUuid) {
        ChatSession session = chatSessionRepository.findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiên chat tương ứng!"));

        String key = String.format(DRAFT_KEY_PATTERN, userId, sessionUuid);
        String finalContent = safeGet(key, () -> {
            String dbContent = session.getHtmlContent();
            if (dbContent == null || dbContent.isBlank()) dbContent = session.getEditorContent();
            return dbContent != null ? dbContent : "";
        });

        if (finalContent == null || finalContent.isBlank()) {
            throw new IllegalArgumentException("Không có dữ liệu nào để hoàn tất xử lý!");
        }

        String aiResponse = "Kết quả AI xử lý từ đoạn text cuối cùng: " + finalContent;

        session.setHtmlContent(finalContent);
        chatSessionRepository.save(session);

        safeDelete(key);

        return aiResponse;
    }

    private ChatSessionDTO convertToDTO(ChatSession session) {
        return ChatSessionDTO.builder()
                .sessionUuid(session.getSessionUuid())
                .tagId(session.getTagId())
                .sessionName(session.getSessionName())
                .currentEditorContent(session.getEditorContent())
                .createdAt(session.getCreatedAt())
                .editorContent(session.getEditorContent())
                .assistantId(session.getAssistant() != null ? session.getAssistant().getAssistantId() : null)
                .build();
    }

    public List<ChatSessionDTO> getSessionsByUser(
        String userId,
        Integer assistantId
    ) {
        List<ChatSession> sessions =
                chatSessionRepository
                        .findByUser_UserIdAndAssistant_AssistantId(
                                userId,
                                assistantId
                        );

        return sessions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void deleteSession(String sessionUuid, String userId) {
        ChatSession session = chatSessionRepository.findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() -> new NoSuchElementException("Session not found or you don't have permission to delete it"));
        chatSessionRepository.delete(session);
    }

    public void saveAiResult(Integer sessionId, String userId, String content) {
        ChatSession session = chatSessionRepository.findBySessionIdAndUser_UserId(sessionId, userId)
                .orElseThrow(() -> new NoSuchElementException("Session not found or you don't have permission to update it"));

        session.setEditorContent(content);
        session.setStatus(SessionStatus.EDITING);
        chatSessionRepository.save(session);

        DraftStateDTO state = new DraftStateDTO();
        state.setSessionUuid(session.getSessionUuid());
        state.setEditorText(content);
        state.setUserId(userId);
        state.setStatus("EDITING");
        draftRepository.save(state);
    }

    public SessionStatus markEditing(Integer sessionId, String userId) {
        ChatSession session = chatSessionRepository.findBySessionIdAndUser_UserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session không tồn tại hoặc không thuộc user"));

        session.setStatus(ChatSession.SessionStatus.EDITING);
        chatSessionRepository.save(session);
        return session.getStatus();
    }

    public ChatSessionDTO updateSessionName(String userId, String sessionUuid, String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionName không được để trống");
        }
        ChatSession session = chatSessionRepository.findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() -> new NoSuchElementException("Session not found or you don't have permission to update it"));

        session.setSessionName(sessionName.trim());
        ChatSession saved = chatSessionRepository.save(session);
        return convertToDTO(saved);
    }
}