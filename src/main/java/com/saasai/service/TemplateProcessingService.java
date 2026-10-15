package com.saasai.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.saasai.repository.ChatSessionRepository;
import com.saasai.repository.FileMetadataRepository;
import com.saasai.repository.UserRepository;
import com.saasai.entity.FileMetadata;
import com.saasai.entity.ChatSession;
import com.saasai.entity.User;
import com.saasai.feature.ai.AiCompletionService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateProcessingService {

        private final FileMetadataRepository fileUploadRepository;
        private final ChatSessionRepository chatSessionRepository;
        private final UserRepository userRepository;
        private final AiCompletionService aiCompletionService;

        @Async("templateTaskExecutor")
        public void submit(
                        String fileId,
                        Integer sessionId,
                        String userId) {

                try {
                        FileMetadata file = fileUploadRepository
                                        .findById(fileId)
                                        .orElseThrow(() -> new IllegalStateException(
                                                        "Không tìm thấy file: " + fileId));

                        if (file.getCategory() != FileMetadata.FileCategory.TEMPLATE) {
                                return;
                        }

                        String normalizedText = file.getNormalizedText();

                        if (normalizedText == null || normalizedText.isBlank()) {
                                return;
                        }

                        User user = userRepository.findByIdWithAdminPackageConfig(userId)
                                        .orElseThrow(() -> new IllegalStateException(
                                                        "Không tìm thấy user: " + userId));

                        ChatSession session = chatSessionRepository
                                        .findById(sessionId)
                                        .orElseThrow(() -> new IllegalStateException(
                                                        "Không tìm thấy session: " + sessionId));

                        String templateJson = aiCompletionService.extractTemplateFromRawText(
                                        user,
                                        normalizedText);

                        if (templateJson == null || templateJson.isBlank()) {
                                throw new IllegalStateException(
                                                "Không tạo được template definition");
                        }

                        session.setTemplateDefinitionJson(templateJson);
                        chatSessionRepository.save(session);

                        log.info(
                                        "Template xử lý thành công: fileId={}, sessionId={}",
                                        fileId,
                                        sessionId);

                } catch (Exception exception) {
                        log.error(
                                        "Template xử lý bất đồng bộ thất bại: fileId={}",
                                        fileId,
                                        exception);
                }
        }
}