package com.saasai.feature.ai;

import com.saasai.feature.ai.AiCompletionRequestDTO;
import com.saasai.feature.ai.AiStreamResponseDTO;
import com.saasai.feature.ai.ApiResponseDTO;
import com.saasai.feature.ai.AiProviderResultDTO;
import com.saasai.entity.ChatSession;
import com.saasai.entity.User;
import com.saasai.repository.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.NoSuchElementException;

@Service
public class AiService {

    private final AiCompletionService aiCompletionService;
    private final ChatSessionRepository chatSessionRepository;

    @Autowired
    public AiService(
            AiCompletionService aiCompletionService,
            ChatSessionRepository chatSessionRepository
    ) {
        this.aiCompletionService = aiCompletionService;
        this.chatSessionRepository = chatSessionRepository;
    }

    /**
     * Entry point cho streaming completion.
     * Core logic nằm ở AiCompletionService.
     */
    public AiProviderResultDTO processCompletion(
            User user,
            AiCompletionRequestDTO request
    ) {
        return aiCompletionService.complete(user, request);
    }

    public byte[] exportDocument(String sessionUuid, String userId, String format) {
        ChatSession session = chatSessionRepository
                .findBySessionUuidAndUser_UserId(sessionUuid, userId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        String content = session.getHtmlContent();
        if (content == null || content.isBlank()) {
            content = session.getEditorContent();
        }
        if (content == null) {
            content = "";
        }

        String normalizedFormat = format == null ? "TXT" : format.trim().toUpperCase();

        return switch (normalizedFormat) {
            case "TXT" -> content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            case "DOCX" -> ("Document export fallback for DOCX\n\n" + content)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            case "PDF" -> ("Document export fallback for PDF\n\n" + content)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            default -> ("Unsupported format " + normalizedFormat + ". Export content:\n\n" + content)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        };
    }

    public void processCompletion(int i, String testUserId, Object object, String string, boolean b, String string2,
            SseEmitter emitter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processCompletion'");
    }
}