package com.saasai.feature.ai;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiResourceService {

        private static final String ROOT_PATH = "ai-resources";

        private static final String COMMON_PROMPT_FILE = "Prompt_common.txt";
        private static final String ASSIST_PROMPT_FILE = "PROMPT_ASSIST.txt";

        // Tải prompt cho một chủ đề cụ thể
        public String loadTopicPrompt(String topicCode) {
                String safeTopicCode = validateCode(topicCode);

                String pattern = "classpath*:ai-resources/topics/"
                                + safeTopicCode
                                + "/PROMPT_*.txt";

                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

                try {
                        Resource[] resources = resolver.getResources(pattern);

                        for (Resource resource : resources) {
                                String filename = resource.getFilename();

                                if (filename == null
                                                || filename.equals("PROMPT_ASSIST.txt")) {
                                        continue;
                                }

                                return resource
                                                .getContentAsString(StandardCharsets.UTF_8)
                                                .trim();
                        }

                        throw new IllegalStateException(
                                        "Không tìm thấy prompt topic cho "
                                                        + safeTopicCode);

                } catch (IOException exception) {
                        throw new IllegalStateException(
                                        "Không thể đọc prompt topic cho "
                                                        + safeTopicCode,
                                        exception);
                }
        }

        // Resolve a template for a given topic and template code.
        public ResolvedTemplate resolveTemplate(
                        String topicCode,
                        String templateCode) {
                String safeTopicCode = validateCode(topicCode);
                String safeTemplateCode = validateCode(templateCode);

                String pattern = "classpath*:ai-resources/topics/"
                                + safeTopicCode
                                + "/templates/**/*.txt";

                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

                try {
                        Resource[] resources = resolver.getResources(pattern);

                        for (Resource resource : resources) {
                                String filename = resource.getFilename();

                                if (filename == null
                                                || !filename.equals(safeTemplateCode + ".txt")) {
                                        continue;
                                }

                                String templateContent = resource
                                                .getContentAsString(StandardCharsets.UTF_8)
                                                .trim();

                                String groupPrompt = loadSiblingGroupPrompt(resource);

                                return new ResolvedTemplate(
                                                filename,
                                                templateContent,
                                                groupPrompt);
                        }

                        log.error(
                                        "Không tìm thấy template '{}', topic '{}'. Sẽ dùng template MAU_CHUNG.",
                                        safeTemplateCode,
                                        safeTopicCode);

                        for (Resource resource : resources) {
                                String filename = resource.getFilename();

                                if (filename == null
                                                || !filename.contains("MAU_CHUNG")
                                                || !filename.endsWith(".txt")) {
                                        continue;
                                }

                                String templateContent = resource
                                                .getContentAsString(StandardCharsets.UTF_8)
                                                .trim();

                                String groupPrompt = loadSiblingGroupPrompt(resource);

                                return new ResolvedTemplate(
                                                filename,
                                                templateContent,
                                                groupPrompt);
                        }

                        throw new IllegalStateException(
                                        "Không tìm thấy template "
                                                        + safeTemplateCode
                                                        + " và không có file MAU_CHUNG trong topic "
                                                        + safeTopicCode);

                } catch (IOException exception) {
                        throw new IllegalStateException(
                                        "Không thể đọc template "
                                                        + safeTemplateCode
                                                        + " trong topic "
                                                        + safeTopicCode,
                                        exception);
                }
        }

        // 1. Load the common prompt for the AI assistant.
        // ai-resources/global/Prompt_common.txt
        public String loadCommonPrompt() {
                return readText(
                                ROOT_PATH
                                                + "/global/" + COMMON_PROMPT_FILE);
        }

        // 2. Load the assist prompt for the AI assistant.
        // ai-resources/topics/Prompt_assist.txt
        public String loadAssistPrompt() {
                return readText(
                                ROOT_PATH
                                                + "/topics/"
                                                + ASSIST_PROMPT_FILE);
        }

        // 3. Load a specific template for a given topic and template code.
        // example ai-resources/topics/BAO_CAO/templates/A/BAO_CAO_DANH_GIA_DINH_KY.txt
        public String loadTemplate(
                        String topicCode,
                        String templateCode) {
                return resolveTemplate(
                                topicCode,
                                templateCode).templateContent();
        }

        public String getRulesPath(String topicCode) {
                String safeTopicCode = validateCode(topicCode);

                return ROOT_PATH
                                + "/topics/"
                                + safeTopicCode
                                + "/rules.json";
        }

        private String readText(String resourcePath) {
                ClassPathResource resource = new ClassPathResource(resourcePath);

                if (!resource.exists()) {
                        throw new IllegalStateException(
                                        "Không tìm thấy AI resource: "
                                                        + resourcePath);
                }

                try {
                        return resource
                                        .getContentAsString(StandardCharsets.UTF_8)
                                        .trim();

                } catch (IOException exception) {
                        throw new IllegalStateException(
                                        "Không thể đọc AI resource: "
                                                        + resourcePath,
                                        exception);
                }
        }

        private String validateCode(String code) {
                if (code == null
                                || !code.matches("^[A-Z0-9_]+$")) {

                        throw new IllegalArgumentException(
                                        "Resource code không hợp lệ: " + code);
                }

                return code;
        }

        // Load an optional group prompt for a specific topic and group. Returns an
        // empty string if the group prompt does not exist.
        public String loadOptionalGroupPrompt(
                        String topicCode,
                        String groupCode) {
                if (groupCode == null || groupCode.isBlank()) {
                        return "";
                }

                String safeTopicCode = validateCode(topicCode);
                String safeGroupCode = validateCode(groupCode);

                String resourcePath = ROOT_PATH
                                + "/topics/"
                                + safeTopicCode
                                + "/templates/"
                                + safeGroupCode
                                + "/PROMPT_" + safeGroupCode + ".txt";

                ClassPathResource resource = new ClassPathResource(resourcePath);

                if (!resource.exists()) {
                        return "";
                }

                try {
                        return resource
                                        .getContentAsString(StandardCharsets.UTF_8)
                                        .trim();

                } catch (IOException exception) {
                        throw new IllegalStateException(
                                        "Không thể đọc group prompt: "
                                                        + resourcePath,
                                        exception);
                }
        }

        // Load the group prompt that is a sibling of the given template resource.
        // Returns an empty string if the prompt does not exist.
        private String loadSiblingGroupPrompt(Resource templateResource) {
                try {
                        String templateFileName = templateResource.getFilename();

                        if (templateFileName == null || !templateFileName.endsWith(".txt")) {
                                return "";
                        }

                        String groupName = templateResource
                                        .getURL()
                                        .getPath()
                                        .replace('\\', '/')
                                        .split("/templates/")[1]
                                        .split("/")[0];

                        Resource promptResource = templateResource.createRelative(
                                        "PROMPT_" + groupName + ".txt");

                        if (!promptResource.exists()) {
                                promptResource = templateResource.createRelative("PROMPT.txt");
                        }

                        if (!promptResource.exists()) {
                                return "";
                        }

                        return promptResource
                                        .getContentAsString(StandardCharsets.UTF_8)
                                        .trim();

                } catch (IOException exception) {
                        throw new IllegalStateException(
                                        "Không thể đọc group prompt",
                                        exception);
                }
        }

}