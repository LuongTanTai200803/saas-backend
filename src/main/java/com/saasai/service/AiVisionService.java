package com.saasai.service;

import com.saasai.feature.ai.AiProviderResultDTO;
import com.saasai.feature.ai.client.OpenRouterClient;
import com.saasai.feature.ai.openrouter.OpenRouterVisionMessageDTO;
import com.saasai.feature.ai.openrouter.OpenRouterVisionRequestDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class AiVisionService {

        private static final Logger log = LoggerFactory.getLogger(AiVisionService.class);
        @Value("${ai.max-tokens:15000}")
        private int maxTokens;

        private final OpenRouterClient openRouterClient;
        private final String prompt;
        private final String model;

        public AiVisionService(
                        OpenRouterClient openRouterClient,
                        ResourceLoader resourceLoader,
                        @Value("${openrouter.vision-model}") String model) throws IOException {

                this.openRouterClient = openRouterClient;
                this.model = model;

                Resource resource = resourceLoader.getResource(
                                "classpath:ai-resources/global/"
                                                + "PROMPT_SCAN_TEXT.txt");

                if (!resource.exists()) {
                        throw new IllegalStateException(
                                        "Không tìm thấy PROMPT_SCAN_TEXT.txt");
                }

                this.prompt = resource.getContentAsString(
                                StandardCharsets.UTF_8);
        }

        public String extractText(
                        List<Path> imagePaths) throws IOException {

                if (imagePaths == null || imagePaths.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Không có ảnh để gửi cho AI");
                }

                List<Object> userContent = new ArrayList<>();

                userContent.add(Map.of(
                                "type", "text",
                                "text", "Đọc toàn bộ nội dung trong các ảnh "
                                                + "theo đúng thứ tự trang."));

                for (Path imagePath : imagePaths) {
                        byte[] imageBytes = Files.readAllBytes(imagePath);

                        String contentType = resolveContentType(imagePath);

                        String base64 = Base64.getEncoder()
                                        .encodeToString(imageBytes);

                        userContent.add(Map.of(
                                        "type", "image_url",
                                        "image_url", Map.of(
                                                        "url",
                                                        "data:"
                                                                        + contentType
                                                                        + ";base64,"
                                                                        + base64)));
                }

                OpenRouterVisionRequestDTO request = new OpenRouterVisionRequestDTO(
                                model,
                                List.of(
                                                new OpenRouterVisionMessageDTO(
                                                                "system",
                                                                prompt),
                                                new OpenRouterVisionMessageDTO(
                                                                "user",
                                                                userContent)),
                                0.1,
                                maxTokens,
                                false);

                AiProviderResultDTO result = openRouterClient.completeVision(request);

                if (result == null
                                || result.content() == null
                                || result.content().isBlank()) {
                        throw new IllegalStateException(
                                        "AI không trả về nội dung văn bản");
                }

                log.info(
                                "AI Vision response: model={}, finishReason={}, "
                                                + "textLength={}, promptTokens={}, completionTokens={}, "
                                                + "totalTokens={}",
                                model,
                                result.finishReason(),
                                result.content() != null
                                                ? result.content().length()
                                                : null,
                                result.promptTokens(),
                                result.completionTokens(),
                                result.totalTokens());

                return result.content();

        }

        private String resolveContentType(
                        Path imagePath) {
                String fileName = imagePath.getFileName()
                                .toString()
                                .toLowerCase();

                if (fileName.endsWith(".jpg")
                                || fileName.endsWith(".jpeg")) {
                        return "image/jpeg";
                }

                return "image/png";
        }
}