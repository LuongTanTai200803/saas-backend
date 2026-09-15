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
 * <p>Service này KHÔNG:
 * <ul>
 *     <li>Không chọn model.</li>
 *     <li>Không đọc file resource.</li>
 *     <li>Không routing template.</li>
 * </ul>
 *
 * <p>Service này CHỈ có nhiệm vụ:
 * <ul>
 *     <li>Ghép System Instruction.</li>
 *     <li>Ghép Prompt của Topic.</li>
 *     <li>Ghép Template được chọn.</li>
 *     <li>Ghép dữ liệu người dùng nhập.</li>
 *     <li>Ghép nội dung file upload (nếu có).</li>
 *     <li>Tạo danh sách Message gửi OpenRouter.</li>
 * </ul>
 */
@Service
public class PromptBuilderService {

    /**
     * Xây dựng danh sách messages theo định dạng OpenRouter.
     *
     * @param template          Template đã được routing.
     * @param editorText        Nội dung người dùng đã nhập, được lấy từ chat_session.editorText.
     * @param formData          FormData người dùng đã nhập, được lấy từ chat_session.formData.
     * @param uploadedFileText  Nội dung file đã extract (có thể rỗng).
     * @return Danh sách message gửi AI.
     */
    
    public List<OpenRouterMessageDTO> buildMessages(
        AiTemplateContent template,
        DocumentFormDTO formData,
        String promptTemplate,
        String uploadedFileText
        ) {
        String systemContent = buildSystemContent(template, promptTemplate);
        String userContent = buildAiContentOnlyPrompt(formData, uploadedFileText);

        return List.of(
                new OpenRouterMessageDTO("system", systemContent),
                new OpenRouterMessageDTO("user", userContent)
        );
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
        String promptTemplate
        ) {
        return Stream.of(
                safe(promptTemplate),
                safe(template.globalSystemInstruction()),
                safe(template.topicPrompt()),
                "## TEMPLATE BODY STRUCTURE",
                safe(template.templateContent())
        ).filter(s -> !s.isBlank())
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
                String uploadedFileText
        ) {
                StringBuilder builder = new StringBuilder();

                builder.append("""
                        ## NHIỆM VỤ
                        Sinh nội dung nghị quyết theo JSON sau:

                        {
                        "doanCanCu": "...",
                        "cacMuc": [
                                {
                                "tieuDeMuc": "Lấy nguyên văn từ TEMPLATE BODY STRUCTURE",
                                "noiDungChiTiet": "..."
                                }
                        ]
                        }

                        QUY TẮC BẮT BUỘC:
                        - TEMPLATE BODY STRUCTURE là schema cao nhất.
                        - Giữ nguyên số lượng mục, thứ tự mục, tieuDeMuc theo TEMPLATE BODY STRUCTURE.
                        - KHÔNG đổi, rút gọn, diễn giải, đặt lại tieuDeMuc.
                        - deCuongDanY chỉ là gợi ý nội dung, KHÔNG phải schema.
                        - Tài liệu đính kèm chỉ để bổ sung/đối chiếu nội dung.
                        - Không tự bịa số liệu/sự kiện/thông tin không có căn cứ.
                        - Nếu thiếu dữ liệu, viết ở mức khái quát hoặc nêu thiếu dữ liệu, không bịa.

                        Chỉ sinh các trường:
                        - doanCanCu
                        - cacMuc
                        - tieuDeMuc
                        - noiDungChiTiet

                        KHÔNG sinh lại các trường thể thức. Các trường thể thức sẽ được render bởi DOCX template.

                        ### Dữ liệu đầu vào
                        Tên văn bản: %s
                        Nội dung chính: %s
                        Đề cương gợi ý (không phải schema): %s
                        Văn bản chỉ đạo: %s
                        Văn bản pháp lý: %s
                        Bảng biểu số liệu: %s
                        """
                        .formatted(
                                safe(formData.getTenVanBan()),
                                safe(formData.getNoiDungChinh()),
                                safe(formData.getDeCuongDanY()),
                                safe(formData.getVanBanChiDao()),
                                safe(formData.getVanBanPhapLy()),
                                safe(formData.getBangBieuSoLieu())
                        ));

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