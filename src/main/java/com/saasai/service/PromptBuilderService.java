package com.saasai.service;

import com.saasai.feature.ai.AiCompletionRequestDTO;
import com.saasai.dto.DocumentFormDTO;
import com.saasai.feature.ai.openrouter.OpenRouterMessageDTO;
import com.saasai.feature.ai.AiTemplateContent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Xây dựng prompt hoàn chỉnh gửi tới AI.
 *
 * <p>
 * Service này KHÔNG:
 * <ul>
 * <li>Không chọn model.</li>
 * <li>Không đọc file resource.</li>
 * <li>Không routing template.</li>
 * </ul>
 *
 * <p>
 * Service này CHỈ có nhiệm vụ:
 * <ul>
 * <li>Ghép System Instruction.</li>
 * <li>Ghép Prompt của Topic.</li>
 * <li>Ghép Template được chọn.</li>
 * <li>Ghép dữ liệu người dùng nhập.</li>
 * <li>Ghép nội dung file upload (nếu có).</li>
 * <li>Tạo danh sách Message gửi OpenRouter.</li>
 * </ul>
 */
@Service
public class PromptBuilderService {

        /**
         * Xây dựng danh sách messages theo định dạng OpenRouter.
         *
         * @param template         Template đã được routing.
         * @param userText         Nội dung người dùng đã nhập, được lấy từ
         *                         chat_session.editorText.
         * @param formData         FormData người dùng đã nhập, được lấy từ
         *                         chat_session.formData.
         * @param uploadedFileText Nội dung file đã extract (có thể rỗng).
         * @return Danh sách message gửi AI.
         */

        public List<OpenRouterMessageDTO> buildMessages(
                        AiTemplateContent template,
                        DocumentFormDTO formData,
                        String promptTemplate,
                        String uploadedFileText,
                        String userText) {
                String systemContent = buildSystemContent(template, promptTemplate);
                String userContent = buildAiContentOnlyPrompt(formData, uploadedFileText, userText);

                return List.of(
                                new OpenRouterMessageDTO("system", systemContent),
                                new OpenRouterMessageDTO("user", userContent));
        }

        /**
         * Xây dựng System Prompt.
         *
         * Bao gồm:
         * - System Instruction toàn hệ thống.
         * - Prompt riêng của Topic.
         */
        private String buildSystemContent(
                        AiTemplateContent template,
                        String promptTemplate) {
                return Stream.of(
                                safe(promptTemplate),
                                safe(template.commonPrompt()),
                                safe(template.assistPrompt()),
                                safe(template.topicPrompt()),
                                safe(template.groupPrompt()),
                                "## TEMPLATE BODY STRUCTURE",
                                safe(template.templateContent()))
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.joining("\n\n"));
        }

        /**
         * Xây dựng User Prompt.
         *
         * Bao gồm:
         * - Thông tin routing.
         * - Toàn bộ dữ liệu Wizard.
         * - Template cơ sở.
         * - Prompt người dùng.
         * - Nội dung file upload.
         */
        private String buildAiContentOnlyPrompt(
                        DocumentFormDTO formData,
                        String uploadedFileText,
                        String userText) {
                StringBuilder builder = new StringBuilder();

                builder.append("""
                                ## DỮ LIỆU ĐẦU VÀO

                                Loại văn bản: %s
                                Tên văn bản: %s

                                Văn bản chỉ đạo:
                                %s

                                Văn bản pháp lý:
                                %s

                                Nội dung chính:
                                %s

                                Bảng biểu số liệu:
                                %s

                                Tài liệu minh chứng:
                                %s

                                Đề cương gợi ý:
                                %s

                                Tạo phụ lục: %s
                                Đối chiếu: %s
                                Bám căn cứ: %s
                                Thể thức: %s


                                ### YÊU CẦU NGƯỜI DÙNG
                                %s
                                """.formatted(
                                safe(formData.getLoaiVanBan()),
                                safe(formData.getTenVanBan()),
                                safe(formData.getVanBanChiDao()),
                                safe(formData.getVanBanPhapLy()),
                                safe(formData.getNoiDungChinh()),
                                safe(formData.getBangBieuSoLieu()),
                                safe(formData.getTaiLieuMinhChung()),
                                safe(formData.getDeCuongDanY()),
                                Boolean.toString(Boolean.TRUE.equals(formData.getTaoPhuLuc())),
                                Boolean.toString(Boolean.TRUE.equals(formData.getDoiChieu())),
                                Boolean.toString(Boolean.TRUE.equals(formData.getBamCanCu())),
                                Boolean.toString(Boolean.TRUE.equals(formData.getTheThuc())),
                                safe(userText)));

                if (StringUtils.hasText(uploadedFileText)) {
                        builder.append("\n\n### TÀI LIỆU ĐÍNH KÈM\n");
                        builder.append(uploadedFileText.trim());
                }

                return builder.toString().trim();
        }

        /**
         * Tránh null khi ghép prompt.
         */
        private String safe(String value) {
                return value == null
                                ? ""
                                : value.trim();
        }

        private String safe(List<String> values) {
                if (values == null || values.isEmpty()) {
                        return "";
                }

                return values.stream()
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.joining("\n"));
        }

}