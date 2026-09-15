package com.saasai.feature.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.saasai.feature.ai.AiCompletionRequestDTO;
import com.saasai.dto.CreditSettleRequestDTO;
import com.saasai.dto.DocumentFormDTO;
import com.saasai.dto.ModelRoute;
import com.saasai.feature.ai.AiProviderResultDTO;
import com.saasai.entity.ChatSession;
import com.saasai.entity.ChatSession.SessionStatus;
import com.saasai.entity.CreditTransaction;
import com.saasai.entity.FileMetadata;
import com.saasai.entity.User;
import com.saasai.feature.ai.client.OpenRouterClient;
import com.saasai.feature.ai.openrouter.OpenRouterMessageDTO;
import com.saasai.feature.ai.openrouter.OpenRouterRequestDTO;
import com.saasai.feature.payment.CreditAccountRepository;
import com.saasai.repository.ChatSessionRepository;
import com.saasai.repository.UserRepository;
import com.saasai.service.ChatSessionFileService;
import com.saasai.service.ChatSessionService;
import com.saasai.service.CreditPricingService;
import com.saasai.service.CreditService;
import com.saasai.service.DraftFileService;
import com.saasai.service.FileTextService;
import com.saasai.service.PackageRoutingService;
import com.saasai.service.PromptBuilderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class AiCompletionService {

        private static final Logger log =
        LoggerFactory.getLogger(AiCompletionService.class);

    private final PackageRoutingService packageRoutingService;
    private final AiTemplateService aiTemplateService;
    private final PromptBuilderService promptBuild;
    private final OpenRouterClient openRouterClient;
    private final DraftFileService draftFileService;
    private final ChatSessionFileService chatSessionFileService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatSessionService chatSessionService;
    private final ObjectMapper objectMapper;
    private final FileTextService fileTextService;
    private final String promptTemplate;

    private final CreditService creditService;

    private final CreditPricingService creditPricingService;

    @Autowired
    private final UserRepository userRepository;

    public record AiPromptContext(
            AiTemplateContent templateContent,
            String promptTemplate,
            String uploadedFileText
    ) {
    }

    public AiCompletionService(
            PackageRoutingService packageRoutingService,
            AiTemplateService aiTemplateService,
            PromptBuilderService promptBuild,
            OpenRouterClient openRouterClient,
            DraftFileService draftFileService,
            ChatSessionFileService chatSessionFileService,
            ChatSessionRepository chatSessionRepository,
            ChatSessionService chatSessionService,
            ObjectMapper objectMapper,
            FileTextService fileTextService,
            CreditService creditService,
            CreditPricingService creditPricingService,
            ResourceLoader resourceLoader,
            UserRepository userRepository
    ) throws IOException {

        this.packageRoutingService = packageRoutingService;
        this.aiTemplateService = aiTemplateService;
        this.promptBuild = promptBuild;
        this.openRouterClient = openRouterClient;
        this.draftFileService = draftFileService;
        this.chatSessionFileService = chatSessionFileService;
        this.chatSessionRepository = chatSessionRepository;
        this.chatSessionService = chatSessionService;
        this.objectMapper = objectMapper;
        this.fileTextService = fileTextService;
        this.creditService = creditService;
        this.creditPricingService = creditPricingService;
        this.userRepository = userRepository;

        Resource resource = resourceLoader.getResource(
                "classpath:ai-resources/global/Prompt_common.txt"
        );

        this.promptTemplate =
                resource.getContentAsString(StandardCharsets.UTF_8);
    }

    // =========================================================
    // COMPLETE
    // =========================================================

    public AiProviderResultDTO complete(
            User user,
            AiCompletionRequestDTO input
    ) {

        validateInput(input);

        String sessionUuid = input.getSessionUuid();

        if (sessionUuid == null) {
            throw new IllegalArgumentException(
                    "sessionUuid không được để trống"
            );
        }

        // 1. Validate session ownership
        ChatSession session =
                chatSessionRepository
                        .findBySessionUuidAndUser_UserId(
                                sessionUuid,
                                user.getUserId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Session không tồn tại hoặc không thuộc user"
                                )
                        );

        // Integer sessionId = session.getSessionId();

        if (session.getStatus() == SessionStatus.CLOSED) {
            throw new IllegalArgumentException(
                    "Session đã bị đóng, không thể gửi yêu cầu AI"
            );
        }

        // 4. Lấy FormData từ draft
        String wizardStateJson =
                draftFileService.getFormDataBySession(
                        session.getSessionUuid(),
                        user.getUserId()
                );

        if (wizardStateJson == null || wizardStateJson.isBlank()) {
            throw new IllegalArgumentException(
                    "Draft không có dữ liệu để gọi AI"
            );
        }

        DocumentFormDTO formData;

        try {
            formData = objectMapper.readValue(
                    wizardStateJson,
                    DocumentFormDTO.class
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "wizardStateJson của draft không chứa FormData JSON hợp lệ",
                    e
            );
        }

        validateFormData(formData);

        // 7. Model
        String modelOption = input.getModel();
        ModelRoute modelRoute = packageRoutingService.resolveRoute(user, modelOption);

        // 8 + 9 + 10. Template + file context
        AiPromptContext context =
                buildAiPromptContext(
                        session,
                        user,
                        formData
                );

        // 11. Build prompt riêng cho COMPLETE
        List<OpenRouterMessageDTO> messages =
                buildCompletePrompt(
                        context,
                        formData
                );

        // 12 + 13 + 14 + 16
        return executeAI(
                session,
                user,
                modelRoute,
                messages
        );
    }

    // =========================================================
    // REFINE
    // =========================================================

        public AiProviderResultDTO refineWorkspace(
                User user,
                AiCompletionRequestDTO input
        ) {

        validateRefineInput(input);

        String sessionUuid = input.getSessionUuid();

        // 1. Lấy session
        ChatSession session =
                chatSessionRepository
                        .findBySessionUuidAndUser_UserId(
                                sessionUuid,
                                user.getUserId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Session không tồn tại hoặc không thuộc user"
                                )
                        );

        if (session.getStatus() == SessionStatus.CLOSED) {
                throw new IllegalArgumentException(
                        "Session đã bị đóng, không thể refine"
                );
        }

        // 1. Lấy editorText
        String editorText =
                draftFileService.getEditorTextBySession(
                        session.getSessionUuid(),
                        user.getUserId()
                );

        if (editorText == null || editorText.isBlank()) {
                editorText = session.getEditorContent();
        }

        if (editorText == null || editorText.isBlank()) {
                throw new IllegalArgumentException(
                        "Editor không có nội dung để refine"
                );
        }

        // 2. Lấy FormData từ wizardStateJson
        String wizardStateJson =
                draftFileService.getFormDataBySession(
                        session.getSessionUuid(),
                        user.getUserId()
                );

        if (wizardStateJson == null || wizardStateJson.isBlank()) {
        throw new IllegalArgumentException(
                "Draft không có FormData để xác định template"
        );
        }

        DocumentFormDTO formData;

        try {
        formData = objectMapper.readValue(
                wizardStateJson,
                DocumentFormDTO.class
        );
        } catch (Exception e) {
        throw new IllegalArgumentException(
                "wizardStateJson không chứa DocumentFormDTO JSON hợp lệ",
                e
        );
        }

        // 3. Validate loaiVanBan + tenVanBan
        validateFormData(formData);

        // 4. Build common context
        AiPromptContext context =
                buildAiPromptContext(
                        session,
                        user,
                        formData
                );

        // 5. Build refine prompt
        List<OpenRouterMessageDTO> messages =
                buildRefinePrompt(
                        context,
                        editorText,
                        input.getUserText()
                );

        // 6. Model
        ModelRoute modelRoute =
                packageRoutingService.resolveRoute(user, input.getModel());

        // Gọi AI + save result + mark EDITING + response
        return executeAI(
                session,
                user,
                modelRoute,
                messages
        );
        }

    // =========================================================
    // BUILD PROMPT - COMPLETE
    // =========================================================

    private List<OpenRouterMessageDTO> buildCompletePrompt(
            AiPromptContext context,
            DocumentFormDTO formData
    ) {

        return promptBuild.buildMessages(
                context.templateContent(),
                formData,
                context.promptTemplate(),
                context.uploadedFileText()
        );
    }

    // =========================================================
    // BUILD PROMPT - REFINE
    // =========================================================

    private List<OpenRouterMessageDTO> buildRefinePrompt(
            AiPromptContext context,
            String editorText,
            String userText
    ) {

        String systemContent =
                buildRefineSystemContent(context);

        String userContent =
                buildRefineUserContent(
                        editorText,
                        userText
                );

        return List.of(
                new OpenRouterMessageDTO(
                        "system",
                        systemContent
                ),
                new OpenRouterMessageDTO(
                        "user",
                        userContent
                )
        );
    }

    private String buildRefineSystemContent(
            AiPromptContext context
    ) {

        StringBuilder templateBlock =
                new StringBuilder();

        if (context != null
                && context.promptTemplate() != null
                && !context.promptTemplate().isBlank()) {

            templateBlock
                    .append(context.promptTemplate().trim())
                    .append("\n\n");
        }

        if (context != null
                && context.templateContent() != null) {

            if (context.templateContent().templateContent() != null
                    && !context.templateContent()
                            .templateContent()
                            .isBlank()) {

                templateBlock
                        .append(
                                context.templateContent()
                                        .templateContent()
                                        .trim()
                        )
                        .append("\n\n");
            }

            if (context.templateContent().topicPrompt() != null
                    && !context.templateContent()
                            .topicPrompt()
                            .isBlank()) {

                templateBlock
                        .append(
                                context.templateContent()
                                        .topicPrompt()
                                        .trim()
                        )
                        .append("\n\n");
            }
        }

        if (context != null
                && context.uploadedFileText() != null
                && !context.uploadedFileText().isBlank()) {

            templateBlock
                    .append("### TÀI LIỆU ĐÍNH KÈM\n")
                    .append(
                            context.uploadedFileText().trim()
                    )
                    .append("\n\n");
        }

        return """
                Bạn là trợ lý chỉnh sửa văn bản.

                Hãy duy trì đúng cấu trúc, mục, tên mục
                và ý nghĩa theo template dưới đây.

                Chỉ sửa những phần thực sự cần thiết
                theo yêu cầu người dùng.

                Không được thay đổi bố cục, số mục,
                tên mục, thứ tự mục nếu không cần thiết.

                %s
                """.formatted(
                templateBlock.toString().trim()
        );
    }

    private String buildRefineUserContent(
            String editorText,
            String userText
    ) {

        return """
                Nội dung hiện tại:
                %s

                Yêu cầu người dùng:
                %s
                """.formatted(
                editorText.trim(),
                userText.trim()
        );
    }

    // =========================================================
    // COMMON CONTEXT
    // =========================================================

    private AiPromptContext buildAiPromptContext(
            ChatSession session,
            User user,
            DocumentFormDTO formData
    ) {

        if (formData == null) {
            throw new IllegalArgumentException(
                    "FormData không được để trống"
            );
        }

        AiTemplateContent templateContent =
                aiTemplateService.resolve(
                        formData.getLoaiVanBan(),
                        formData.getTenVanBan()
                );

        String uploadedFileText =
                buildUploadedFileText(
                        session,
                        user.getUserId()
                );

        return new AiPromptContext(
                templateContent,
                promptTemplate,
                uploadedFileText
        );
    }

    /**
     * Refine chỉ cần FormData để xác định template.
     * Không dùng FormData làm nội dung chính của prompt.
     */
    private DocumentFormDTO loadFormDataForContext(
            ChatSession session,
            User user
    ) {

        String wizardStateJson =
                draftFileService.getFormDataBySession(
                        session.getSessionUuid(),
                        user.getUserId()
                );

        if (wizardStateJson == null
                || wizardStateJson.isBlank()) {

            throw new IllegalArgumentException(
                    "Draft không có FormData để xác định template"
            );
        }

        try {

            DocumentFormDTO formData =
                    objectMapper.readValue(
                            wizardStateJson,
                            DocumentFormDTO.class
                    );

            validateFormData(formData);

            return formData;

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "wizardStateJson của draft không chứa FormData JSON hợp lệ",
                    e
            );
        }
    }

    // =========================================================
    // COMMON AI EXECUTION
    // =========================================================

    private AiProviderResultDTO executeAI(
            ChatSession session,
            User user,
            ModelRoute modelRoute,
            List<OpenRouterMessageDTO> messages
    ) {

        // compute hold
        double baseHold = creditPricingService.getBaseHoldDefault();
        double creditRate = modelRoute.creditRate() != null ? modelRoute.creditRate() : 1.0;
        double outputWeight = creditPricingService.getOutputWeightDefault();
        double holdAmount = creditPricingService.calculateHoldFromBaseHold(baseHold, creditRate);

        // prepare hold DTO and create transaction
        CreditSettleRequestDTO holdReq = new CreditSettleRequestDTO();
        holdReq.setModel(null);
        holdReq.setModelPackageId(modelRoute.modelPackageId());
        holdReq.setCreditRate(creditRate);
        holdReq.setOutputWeight(outputWeight);
        holdReq.setEstimatedHold(holdAmount);
        holdReq.setInputCredit(0.0);
        holdReq.setOutputCredit(0.0);
        holdReq.setDescription("AI HOLD - session " + session.getSessionUuid());
        holdReq.setSessionUuid(session.getSessionUuid());
        holdReq.setModel(modelRoute.primaryModel());

        CreditTransaction holdTx = creditService.recordHoldTransactionWithSnapshot(user.getUserId(), session.getSessionId(), holdReq);

        // 12. Tạo OpenRouter request
        OpenRouterRequestDTO request =
                new OpenRouterRequestDTO(
                        modelRoute.primaryModel(),
                        messages,
                        modelRoute.temperature(),
                        modelRoute.maxTokens(),
                        false
                );

        try {
        String requestJson =
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(request);

        System.out.println("=== OPENROUTER REQUEST ===");
        System.out.println(requestJson);
        System.out.println("=== END OPENROUTER REQUEST ===");
        } catch (Exception e) {
        System.err.println("Không in được payload OpenRouter: " + e.getMessage());
        e.printStackTrace();
        }

        // 13. Gọi AI
        AiProviderResultDTO aiResult;
        try {
        aiResult = openRouterClient.complete(request);
        } catch (Exception ex) {
        // provider error -> refund full hold and rethrow
        try {
                creditService.refundHold(holdTx.getTransactionId());
        } catch (Exception ignore) {}
        throw ex;
        }

        if (aiResult == null) {
        // provider returned null -> refund and fail
        try {
                creditService.refundHold(holdTx.getTransactionId());
        } catch (Exception ignore) {}
        throw new IllegalStateException("AI không trả về kết quả");
        }

        // 14. On success: compute pricing and settle
        long promptTokens = aiResult.promptTokens();
        long completionTokens = aiResult.completionTokens();
        long totalTokens = aiResult.totalTokens();

        double baseCredit = creditPricingService.calculateBaseCredit(promptTokens, completionTokens, outputWeight);
        double actualCredit = creditPricingService.calculateActualCredit(baseCredit, creditRate);

        // settle (updates same transaction, deducts actual credit and sets refunded)
        creditService.settleTransaction(
                holdTx.getTransactionId(),
                promptTokens,
                completionTokens,
                totalTokens,
                actualCredit,
                aiResult.model()
        );

        // save model into transaction if needed (ensure settleTransaction handles model persist)
        // Chuyển session sang EDITING (save result)
        if (aiResult.content() != null
                && !aiResult.content().isBlank()) {

        try {

                chatSessionService.saveAiResult(
                        session.getSessionId(),
                        user.getUserId(),
                        aiResult.content()
                );

        } catch (Exception e) {

                System.err.println(
                        "Không thể lưu kết quả AI vào session "
                                + session.getSessionId()
                                + ": "
                                + e.getMessage()
                );
        }
        }

        SessionStatus sessionStatus =
                chatSessionService.markEditing(
                        session.getSessionId(),
                        user.getUserId()
                );

        // 16. Response
        return AiProviderResultDTO.builder()
                .content(aiResult.content())
                .model(aiResult.model())
                .finishReason(aiResult.finishReason())
                .promptTokens(aiResult.promptTokens())
                .completionTokens(aiResult.completionTokens())
                .totalTokens(aiResult.totalTokens())
                .sessionStatus(sessionStatus)
                .build();
        }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateInput(
            AiCompletionRequestDTO input
    ) {

        if (input == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu yêu cầu AI không được để trống"
            );
        }

        if (input.getSessionUuid() == null
                || input.getSessionUuid().isBlank()) {

            throw new IllegalArgumentException(
                    "sessionUuid không được để trống"
            );
        }
    }

    private void validateRefineInput(
            AiCompletionRequestDTO input
    ) {

        validateInput(input);

        if (input.getUserText() == null
                || input.getUserText().isBlank()) {

            throw new IllegalArgumentException(
                    "userText không được để trống"
            );
        }
    }

    private void validateFormData(
            DocumentFormDTO formData
    ) {

        if (formData == null) {
            throw new IllegalArgumentException(
                    "FormData trong draft không được để trống"
            );
        }

        if (formData.getLoaiVanBan() == null
                || formData.getLoaiVanBan().isBlank()) {

            throw new IllegalArgumentException(
                    "loaiVanBan không được để trống"
            );
        }

        if (formData.getTenVanBan() == null
                || formData.getTenVanBan().isBlank()) {

            throw new IllegalArgumentException(
                    "tenVanBan không được để trống"
            );
        }
    }

    // =========================================================
    // FILE CONTEXT
    // =========================================================

    private String buildUploadedFileText(
            ChatSession session,
            String userId
    ) {

        List<FileMetadata> files =
                chatSessionFileService.getFilesBySession(
                        session
                );

        if (files.isEmpty()) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        for (int index = 0;
             index < files.size();
             index++) {

            FileMetadata file =
                    files.get(index);

            String text =
                    fileTextService.getNormalizedText(
                            file.getFileId(),
                            userId
                    );

            if (text == null
                    || text.isBlank()) {
                continue;
            }

            builder.append("[UPLOADED_FILE_")
                    .append(index + 1)
                    .append("]\n");

            builder.append("fileName: ")
                    .append(file.getFileName())
                    .append("\n");

            builder.append(text.trim())
                    .append("\n\n");
        }

        return builder
                .toString()
                .trim();
    }
}