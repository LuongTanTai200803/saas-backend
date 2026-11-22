package com.saasai.feature.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.saasai.repository.ChatSessionRepository;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.saasai.entity.ChatSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
                        throw new IllegalArgumentException("Chat session must not be null");
                }

                String templateDefinition = session.getTemplateDefinitionJson();

                if (templateDefinition == null || templateDefinition.isBlank()) {
                        TemplateRouteResult route = templateRoutingService.resolve(topicCode, documentTitle);

                        ResolvedTemplate resolved = aiResourceService.resolveTemplate(
                                        route.topicCode(),
                                        route.templateCode());

                        templateDefinition = markAsSystemTemplate(
                                        resolved.templateContent(),
                                        route.topicCode(),
                                        route.templateCode());

                        session.setTemplateDefinitionJson(templateDefinition);
                        chatSessionRepository.save(session);

                        return new AiTemplateContent(
                                        route.topicCode(),
                                        route.templateCode(),
                                        resolved.templateFileName(),
                                        aiResourceService.loadCommonPrompt(),
                                        aiResourceService.loadAssistPrompt(),
                                        aiResourceService.loadTopicPrompt(route.topicCode()),
                                        templateDefinition,
                                        resolved.groupPrompt());
                }

                // check template origin
                JsonNode definition = readTemplateDefinition(templateDefinition);

                String templateOrigin = definition.path("templateOrigin").asText();
                if (templateOrigin.isBlank()) {
                        templateOrigin = definition.path("source").asText();
                }

                if ("SYSTEM".equalsIgnoreCase(templateOrigin)) {
                        String savedTopicCode = definition.path("topicCode").asText();
                        String savedTemplateCode = definition.path("templateCode").asText();

                        ResolvedTemplate resolved = aiResourceService.resolveTemplate(
                                        savedTopicCode,
                                        savedTemplateCode);

                        if (!resolved.templateFileName().equals(savedTemplateCode + ".txt")) {
                                throw new IllegalStateException(
                                                "Không tìm thấy system template đã lưu: " + savedTemplateCode);
                        }

                        return new AiTemplateContent(
                                        savedTopicCode,
                                        savedTemplateCode,
                                        resolved.templateFileName(),
                                        aiResourceService.loadCommonPrompt(),
                                        aiResourceService.loadAssistPrompt(),
                                        aiResourceService.loadTopicPrompt(savedTopicCode),
                                        templateDefinition,
                                        resolved.groupPrompt());
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

        // Chọn template hệ thống cho session
        @Transactional
        public Map<String, String> selectSystemTemplate(
                        String userId,
                        String sessionUuid,
                        String topicCode,
                        String templateCode) {

                ChatSession session = chatSessionRepository
                                .findBySessionUuidAndUser_UserId(sessionUuid, userId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Session không tồn tại hoặc không thuộc user"));

                if (session.getStatus() == ChatSession.SessionStatus.CLOSED) {
                        throw new IllegalArgumentException("Session đã đóng");
                }

                ResolvedTemplate resolved = aiResourceService.resolveTemplate(topicCode, templateCode);

                if (!templateCode.concat(".txt").equals(resolved.templateFileName())) {
                        throw new IllegalArgumentException("Không tìm thấy mẫu template đã chọn");
                }

                try {
                        JsonNode node = objectMapper.readTree(resolved.templateContent());

                        if (!node.isObject()) {
                                throw new IllegalArgumentException("Nội dung template không phải JSON object");
                        }

                        ObjectNode definition = ((ObjectNode) node).deepCopy();
                        definition.put("source", "SYSTEM");
                        definition.put("topicCode", topicCode);
                        definition.put("templateCode", templateCode);

                        session.setTemplateDefinitionJson(
                                        objectMapper.writeValueAsString(definition));
                        chatSessionRepository.save(session);

                        return Map.of(
                                        "sessionUuid", sessionUuid,
                                        "source", "SYSTEM",
                                        "topicCode", topicCode,
                                        "templateCode", templateCode);
                } catch (JsonProcessingException exception) {
                        throw new IllegalStateException(
                                        "Không thể đọc JSON của template " + templateCode,
                                        exception);
                }
        }

        // Mark template as system template
        private String markAsSystemTemplate(
                        String templateJson,
                        String topicCode,
                        String templateCode) {

                JsonNode node = readTemplateDefinition(templateJson);

                if (!node.isObject()) {
                        throw new IllegalStateException("Template phải là JSON object");
                }

                ObjectNode definition = ((ObjectNode) node).deepCopy();

                String source = definition.path("source").asText();

                if (!"SYSTEM".equalsIgnoreCase(source)
                                && !"USER_UPLOAD".equalsIgnoreCase(source)) {
                        definition.put("source", "SYSTEM");
                }

                definition.put("templateOrigin", "SYSTEM");
                definition.put("topicCode", topicCode);
                definition.put("templateCode", templateCode);

                try {
                        return objectMapper.writeValueAsString(definition);
                } catch (JsonProcessingException exception) {
                        throw new IllegalStateException("Không thể lưu JSON template", exception);
                }
        }

        private JsonNode readTemplateDefinition(String templateJson) {
                try {
                        return objectMapper.readTree(templateJson);
                } catch (JsonProcessingException exception) {
                        throw new IllegalStateException("Template JSON không hợp lệ", exception);
                }
        }
}