package com.saasai.feature.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.saasai.repository.ChatSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.saasai.entity.ChatSession;

@Service
@RequiredArgsConstructor
public class AiTemplateService {

        private final TemplateRoutingService templateRoutingService;
        private final AiResourceService aiResourceService;
        private final ChatSessionRepository chatSessionRepository;
        private final ObjectMapper objectMapper;

        public AiTemplateContent resolve(
                        ChatSession session,
                        String topicCode,
                        String documentTitle) {
                if (session == null) {
                        throw new IllegalArgumentException(
                                        "Chat session must not be null");
                }

                String templateDefinition = session.getTemplateDefinitionJson();

                if (templateDefinition == null
                                || templateDefinition.isBlank()) {

                        TemplateRouteResult route = templateRoutingService.resolve(
                                        topicCode,
                                        documentTitle);

                        ResolvedTemplate resolvedTemplate = aiResourceService.resolveTemplate(
                                        route.topicCode(),
                                        route.templateCode());

                        templateDefinition = resolvedTemplate.templateContent();

                        try {
                                objectMapper.readTree(templateDefinition);
                        } catch (JsonProcessingException exception) {
                                throw new IllegalStateException(
                                                "Template server không phải JSON hợp lệ: "
                                                                + resolvedTemplate.templateFileName(),
                                                exception);
                        }

                        session.setTemplateDefinitionJson(templateDefinition);
                        chatSessionRepository.save(session);

                        return new AiTemplateContent(
                                        route.topicCode(),
                                        route.templateCode(),
                                        resolvedTemplate.templateFileName(),
                                        aiResourceService.loadCommonPrompt(),
                                        aiResourceService.loadAssistPrompt(),
                                        aiResourceService.loadTopicPrompt(
                                                        route.topicCode()),
                                        templateDefinition,
                                        resolvedTemplate.groupPrompt());
                }

                return new AiTemplateContent(
                                topicCode,
                                "USER_UPLOAD",
                                "USER_UPLOAD",
                                aiResourceService.loadCommonPrompt(),
                                aiResourceService.loadAssistPrompt(),
                                aiResourceService.loadTopicPrompt(topicCode),
                                templateDefinition,
                                "");
        }

        public AiTemplateContent resolve(
                        String topicCode,
                        String documentTitle) {
                throw new IllegalStateException(
                                "Render flow phải cung cấp ChatSession "
                                                + "để resolve template");
        }
}