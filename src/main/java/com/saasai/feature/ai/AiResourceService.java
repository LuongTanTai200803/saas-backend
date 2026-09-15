package com.saasai.feature.ai;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class AiResourceService {

    private static final String ROOT_PATH = "ai-resources";

    public String loadGlobalSystemInstruction() {
        return readText(
                ROOT_PATH + "/global/SYSTEM_INSTRUCTION.txt"
        );
    }

    public String loadTopicPrompt(String topicCode) {
        String safeTopicCode = validateCode(topicCode);

        return readText(
                ROOT_PATH
                        + "/topics/"
                        + safeTopicCode
                        + "/PROMPT.txt"
        );
    }

    public String loadTemplate(
            String topicCode,
            String templateCode
    ) {
        String safeTopicCode = validateCode(topicCode);
        String safeTemplateCode = validateCode(templateCode);

        return readText(
                ROOT_PATH
                        + "/topics/"
                        + safeTopicCode
                        + "/templates/"
                        + safeTemplateCode
                        + ".txt"
        );
    }

    public String getRulesPath(String topicCode) {
        String safeTopicCode = validateCode(topicCode);

        return ROOT_PATH
                + "/topics/"
                + safeTopicCode
                + "/rules.json";
    }

    private String readText(String resourcePath) {
        ClassPathResource resource =
                new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Không tìm thấy AI resource: "
                            + resourcePath
            );
        }

        try {
            return resource
                    .getContentAsString(StandardCharsets.UTF_8)
                    .trim();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Không thể đọc AI resource: "
                            + resourcePath,
                    exception
            );
        }
    }

    private String validateCode(String code) {
        if (code == null
                || !code.matches("^[A-Z0-9_]+$")) {

            throw new IllegalArgumentException(
                    "Resource code không hợp lệ: " + code
            );
        }

        return code;
    }
}