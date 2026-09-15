package com.saasai.service;



import com.saasai.config.DocumentParserRules;
import com.saasai.config.DocumentRules;
import com.saasai.dto.BlockType;
import com.saasai.dto.DocumentBlock;
import com.saasai.dto.DocumentFormDTO;
import com.saasai.feature.ai.AiTemplateContent;
import com.saasai.feature.ai.AiTemplateService;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.saasai.feature.ai.openrouter.OpenRouterMessageDTO;


// import static org.mockito.ArgumentMatchers.matches;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentRenderServiceImpl implements DocumentRenderService {

        private final DocumentRules documentRules;
        private final DocumentParserRules documentParserRules;
        private final AiTemplateService aiTemplateService;
        private final PromptBuilderService promptBuilderService;

    public DocumentRenderServiceImpl(
        DocumentRules documentRules,
        DocumentParserRules documentParserRules,
        AiTemplateService aiTemplateService,
        PromptBuilderService promptBuilderService
    ) {
        this.documentRules = documentRules;
        this.documentParserRules = documentParserRules;
        this.aiTemplateService = aiTemplateService;
        this.promptBuilderService = promptBuilderService;
    }

    @Override
    public String render(DocumentFormDTO formData) {
        // Use topic/template routing to obtain template
        AiTemplateContent template = aiTemplateService.resolve(
                formData.getLoaiVanBan(),
                formData.getTenVanBan()
        );

        // Build messages; pass empty uploadedFileText (do NOT include file normalized text)
        List<OpenRouterMessageDTO> messages = promptBuilderService.buildMessages(
                template,
                formData,
                "",
                ""
        );

        // The user message contains the filled template + Wizard data
        return messages.stream()
                .filter(m -> "user".equalsIgnoreCase(m.role()))
                .map(OpenRouterMessageDTO::content)
                .findFirst()
                .orElse("");
    }

       @Override
        public List<DocumentBlock> parse(String documentText) {
        
        // System.out.println("titlePattern = " + documentParserRules.titlePattern);
        // System.out.println("headingPattern = " + documentParserRules.headingPattern);
        // System.out.println("sectionPattern = " + documentParserRules.sectionPattern);
        // System.out.println("subSectionPattern = " + documentParserRules.subSectionPattern);
        // System.out.println("recipientPattern = " + documentParserRules.recipientPattern);
        // System.out.println("signaturePattern = " + documentParserRules.signaturePattern);
        // System.out.println("headerLeftPattern = " + documentParserRules.headerLeftPattern);
        // System.out.println("headerRightPattern = " + documentParserRules.headerRightPattern);

        List<DocumentBlock> blocks = new ArrayList<>();

        if (documentText == null || documentText.isBlank()) {
                return blocks;
        }

        String[] lines = documentText.split("\\r?\\n");


        for (int i = 0; i < lines.length; i++) {

                String text = lines[i].trim();

                if (text.isBlank()) {
                continue;
                }

                // =====================================================
                // 0. CLEAN CODE FENCE
                // =====================================================
                if (text.equals("```plaintext") || text.equals("```")) {
                continue;
                }

                if (text.isBlank()) {
                continue;
                }

                // =====================================================
                // 1. HEADER - TWO COLUMNS
                // =====================================================
                String[] columns = splitTwoColumns(text);

                if (columns != null
                        && matches(
                                documentParserRules.headerLeftPattern,
                                columns[0]
                        )
                        && matches(
                                documentParserRules.headerRightPattern,
                                columns[1]
                        )) {

                blocks.add(
                        new DocumentBlock(
                                null,
                                BlockType.HEADER,
                                columns[0],
                                columns[1]
                        )
                );

                continue;
                }

                // =====================================================
                // 2. HEADER DATE - TWO COLUMNS
                // =====================================================
                if (columns != null
                        && matches(
                                documentParserRules.headerDateLeftPattern,
                                columns[0]
                        )
                        && matches(
                                documentParserRules.headerDateRightPattern,
                                columns[1]
                        )) {

                blocks.add(
                        new DocumentBlock(
                                null,
                                BlockType.HEADER_META,
                                columns[0],
                                columns[1]
                        )
                );

                continue;
                }

                // =====================================================
                // RECIPIENT + SIGNATURE → 2 CỘT CUỐI
                // =====================================================
                if (matches(
                        documentParserRules.recipientPattern,
                        text
                )) {

                List<String> leftLines = new ArrayList<>();
                List<String> rightLines = new ArrayList<>();

                // RECIPIENT bắt đầu ở đây
                // leftLines.add(text);

                // Tìm SIGNATURE phía sau
                int signatureIndex = -1;

                for (int j = i + 1; j < lines.length; j++) {

                        String nextText = lines[j].trim();

                        if (matches(
                                documentParserRules.signaturePattern,
                                nextText
                        )) {
                        signatureIndex = j;
                        break;
                        }
                }

                // Không tìm thấy SIGNATURE
                if (signatureIndex == -1) {

                        blocks.add(
                                new DocumentBlock(
                                        text,
                                        BlockType.RECIPIENT,
                                        null,
                                        null
                                )
                        );

                        continue;
                }

                // =================================================
                // LEFT: từ RECIPIENT đến trước SIGNATURE
                // =================================================
                for (int j = i; j < signatureIndex; j++) {

                        String leftText = lines[j].trim();

                        if (!leftText.isBlank()) {
                        leftLines.add(
                                leftText
                        );
                        }
                }

                // =================================================
                // RIGHT: từ SIGNATURE đến cuối
                // =================================================
                for (int j = signatureIndex; j < lines.length; j++) {

                        String rightText = lines[j].trim();

                        if (!rightText.isBlank()) {
                        rightLines.add(
                                rightText
                        );
                        }
                }

                blocks.add(
                        new DocumentBlock(
                                null,
                                BlockType.RECIPIENT_SIGNATURE,
                                String.join("\n", leftLines),
                                String.join("\n", rightLines)
                        )
                );

                // Vì toàn bộ phần cuối đã được gom
                i = lines.length - 1;

                continue;
                }

                // =====================================================
                // 6. TITLE
                // =====================================================
                if (matches(
                        documentParserRules.titlePattern,
                        text
                )) {

                blocks.add(
                        new DocumentBlock(
                                text,
                                BlockType.TITLE,
                                null,
                                null
                        )
                );

                continue;
                }

                // =====================================================
                // 7. TITLE SUB
                // =====================================================
                if (matches(
                        documentParserRules.titleSubPattern,
                        text
                )) {

                blocks.add(
                        new DocumentBlock(
                                text,
                                BlockType.TITLE_SUB,
                                null,
                                null
                        )
                );

                continue;
                }

                // =====================================================
                // 8. HEADING
                // =====================================================
                if (matches(
                        documentParserRules.headingPattern,
                        text
                )) {

                blocks.add(
                        new DocumentBlock(
                                text,
                                BlockType.HEADING,
                                null,
                                null
                        )
                );

                continue;
                }

                // =====================================================
                // 9. SECTION
                // =====================================================
                if (matches(
                        documentParserRules.sectionPattern,
                        text
                )) {

                blocks.add(
                        new DocumentBlock(
                                text,
                                BlockType.SECTION_HEADING,
                                null,
                                null
                        )
                );

                continue;
                }

                // =====================================================
                // 10. SUB SECTION
                // =====================================================
                if (matches(
                        documentParserRules.subSectionPattern,
                        text
                )) {

                blocks.add(
                        new DocumentBlock(
                                text,
                                BlockType.SECTION_HEADING,
                                null,
                                null
                        )
                );

                continue;
                }
                // ======== * ========
                if ("*".equals(text)) {
                blocks.add(
                        new DocumentBlock(
                        text,
                        BlockType.SEPARATOR,
                        null,
                        null
                        )
                );
                continue;
                }

                // =====================================================
                // 11. BODY
                // =====================================================
                blocks.add(
                        new DocumentBlock(
                                text,
                                BlockType.BODY,
                                null,
                                null
                        )
                );
        }

        return blocks;
        }

        private String[] splitTwoColumns(String text) {
        String[] parts = text.split("\\s{3,}", 2);

        if (parts.length != 2) {
                return null;
        }

        return new String[]{
                parts[0].trim(),
                parts[1].trim()
        };
        }

        private boolean matches(String pattern, String text) {
        if (pattern == null || pattern.isBlank()) {
                return false;
        }

        if (text == null) {
                return false;
        }

        return Pattern.matches(pattern, text);
        }

}