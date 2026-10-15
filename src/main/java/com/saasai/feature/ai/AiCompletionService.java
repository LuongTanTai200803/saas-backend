package com.saasai.feature.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.saasai.feature.ai.AiCompletionRequestDTO;
import com.saasai.admin.AdminService;
import com.saasai.dto.ChatMessageDTO;
import com.saasai.dto.CreditSettleRequestDTO;
import com.saasai.dto.DocumentFormDTO;
import com.saasai.dto.ModelRoute;
import com.saasai.feature.ai.AiProviderResultDTO;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.ChatSession;
import com.saasai.entity.ChatSession.SessionStatus;
import com.saasai.entity.CreditTransaction;
import com.saasai.entity.FileMetadata;
import com.saasai.entity.User;
import com.saasai.feature.ai.client.OpenRouterClient;
import com.saasai.feature.ai.openrouter.OpenRouterMessageDTO;
import com.saasai.feature.ai.openrouter.OpenRouterRequestDTO;
import com.saasai.feature.ai.openrouter.OpenRouterVisionMessageDTO;
import com.saasai.feature.ai.openrouter.OpenRouterVisionRequestDTO;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class AiCompletionService {

        private static final Logger log = LoggerFactory.getLogger(AiCompletionService.class);

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
        private final String scanTemplatePrompt;

        private final CreditService creditService;

        private final CreditPricingService creditPricingService;

        private final AdminService adminService;

        @Autowired
        private final UserRepository userRepository;

        public record AiPromptContext(
                        AiTemplateContent templateContent,
                        String promptTemplate,
                        String uploadedFileText) {
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
                        UserRepository userRepository,
                        AdminService adminService) throws IOException {

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
                this.adminService = adminService;

                Resource resource = resourceLoader.getResource(
                                "classpath:ai-resources/global/Prompt_common.txt");

                this.promptTemplate = resource.getContentAsString(StandardCharsets.UTF_8);

                Resource scanResource = resourceLoader.getResource(
                                "classpath:ai-resources/global/PROMPT_SCAN_TEMPLATE.txt");

                this.scanTemplatePrompt = scanResource.getContentAsString(StandardCharsets.UTF_8);
        }

        // =========================================================
        // COMPLETE
        // =========================================================

        public AiProviderResultDTO complete(
                        User user,
                        AiCompletionRequestDTO input) {

                validateInput(input);

                if (input.getUserText() == null
                                || input.getUserText().isBlank()) {
                        throw new IllegalArgumentException(
                                        "userText không được để trống");
                }

                String sessionUuid = input.getSessionUuid();

                if (sessionUuid == null) {
                        throw new IllegalArgumentException(
                                        "sessionUuid không được để trống");
                }

                // 1. Validate session ownership
                ChatSession session = chatSessionRepository
                                .findBySessionUuidAndUser_UserId(
                                                sessionUuid,
                                                user.getUserId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Session không tồn tại hoặc không thuộc user"));

                // Integer sessionId = session.getSessionId();

                if (session.getStatus() == SessionStatus.CLOSED) {
                        throw new IllegalArgumentException(
                                        "Session đã bị đóng, không thể gửi yêu cầu AI");
                }

                // 4. Lấy FormData từ draft
                String wizardStateJson = draftFileService.getFormDataBySession(
                                session.getSessionUuid(),
                                user.getUserId());

                if (wizardStateJson == null || wizardStateJson.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Draft không có dữ liệu để gọi AI");
                }

                DocumentFormDTO formData;

                try {
                        formData = objectMapper.readValue(
                                        wizardStateJson,
                                        DocumentFormDTO.class);
                } catch (Exception e) {
                        throw new IllegalArgumentException(
                                        "wizardStateJson của draft không chứa FormData JSON hợp lệ",
                                        e);
                }

                // 5. Validate loaiVanBan + tenVanBan
                validateFormData(formData);
                // 6. Validate fileIds limit
                validateAiFileIdsLimit(
                                formData.getFileIds(),
                                user);

                // 7. Model
                String modelOption = input.getModel();
                ModelRoute modelRoute = packageRoutingService.resolveRoute(user, modelOption);

                // 8 + 9 + 10. Template + file context
                AiPromptContext context = buildAiPromptContext(
                                session,
                                user,
                                formData,
                                formData.getFileIds());

                // 11. Build prompt riêng cho COMPLETE
                List<OpenRouterMessageDTO> messages = buildCompletePrompt(
                                context,
                                formData,
                                input.getUserText());

                // 12 + 13 + 14 + 16
                return executeAI(
                                session,
                                user,
                                modelRoute,
                                messages,
                                input.getUserText(),
                                context.templateContent().templateFileName());
        }

        // =========================================================
        // REFINE
        // =========================================================

        public AiProviderResultDTO refineWorkspace(
                        User user,
                        AiCompletionRequestDTO input) {

                validateRefineInput(input);

                String sessionUuid = input.getSessionUuid();

                // 1. Lấy session
                ChatSession session = chatSessionRepository
                                .findBySessionUuidAndUser_UserId(
                                                sessionUuid,
                                                user.getUserId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Session không tồn tại hoặc không thuộc user"));

                if (session.getStatus() == SessionStatus.CLOSED) {
                        throw new IllegalArgumentException(
                                        "Session đã bị đóng, không thể refine");
                }

                // 1. Lấy editorText
                String editorText = draftFileService.getEditorTextBySession(
                                session.getSessionUuid(),
                                user.getUserId());

                if (editorText == null || editorText.isBlank()) {
                        editorText = session.getEditorContent();
                }

                if (editorText == null || editorText.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Editor không có nội dung để refine");
                }

                // 2. Lấy FormData từ wizardStateJson
                String wizardStateJson = draftFileService.getFormDataBySession(
                                session.getSessionUuid(),
                                user.getUserId());

                if (wizardStateJson == null || wizardStateJson.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Draft không có FormData để xác định template");
                }

                DocumentFormDTO formData;

                try {
                        formData = objectMapper.readValue(
                                        wizardStateJson,
                                        DocumentFormDTO.class);
                } catch (Exception e) {
                        throw new IllegalArgumentException(
                                        "wizardStateJson không chứa DocumentFormDTO JSON hợp lệ",
                                        e);
                }

                // 3. Validate loaiVanBan + tenVanBan
                validateFormData(formData);

                validateAiFileIdsLimit(
                                input.getFileId(),
                                user);

                // 4. Build common context
                AiPromptContext context = buildAiPromptContext(
                                session,
                                user,
                                formData,
                                input.getFileId());

                // 5. Build refine prompt
                List<OpenRouterMessageDTO> messages = buildRefinePrompt(
                                context,
                                editorText,
                                input.getUserText());

                // 6. Model
                ModelRoute modelRoute = packageRoutingService.resolveRoute(user, input.getModel());

                // Gọi AI + save result + mark EDITING + response
                return executeAI(
                                session,
                                user,
                                modelRoute,
                                messages,
                                input.getUserText(),
                                context.templateContent().templateFileName());
        }

        // =========================================================
        // BUILD PROMPT - COMPLETE
        // =========================================================

        private List<OpenRouterMessageDTO> buildCompletePrompt(
                        AiPromptContext context,
                        DocumentFormDTO formData,
                        String userText) {

                return promptBuild.buildMessages(
                                context.templateContent(),
                                formData,
                                context.promptTemplate(),
                                context.uploadedFileText(),
                                userText);
        }

        // =========================================================
        // BUILD PROMPT - REFINE
        // =========================================================

        private List<OpenRouterMessageDTO> buildRefinePrompt(
                        AiPromptContext context,
                        String editorText,
                        String userText) {

                String systemContent = buildRefineSystemContent(context);

                String userContent = buildRefineUserContent(
                                editorText,
                                userText);

                return List.of(
                                new OpenRouterMessageDTO(
                                                "system",
                                                systemContent),
                                new OpenRouterMessageDTO(
                                                "user",
                                                userContent));
        }

        // BUILD REFINE SYSTEM CONTENT
        // =========================================================
        /*
         * globalSystemInstruction
         * topicPrompt
         * groupPrompt
         * templateContent
         * promptTemplate
         * uploadedFileText
         */
        private String buildRefineSystemContent(
                        AiPromptContext context) {
                StringBuilder templateBlock = new StringBuilder();

                if (context == null) {
                        return "";
                }

                AiTemplateContent template = context.templateContent();

                if (template != null) {
                        appendIfNotBlank(
                                        templateBlock,
                                        template.commonPrompt());

                        appendIfNotBlank(
                                        templateBlock,
                                        template.assistPrompt());

                        appendIfNotBlank(
                                        templateBlock,
                                        template.topicPrompt());

                        appendIfNotBlank(
                                        templateBlock,
                                        template.groupPrompt());

                        appendIfNotBlank(
                                        templateBlock,
                                        template.templateContent());
                }

                if (context.uploadedFileText() != null
                                && !context.uploadedFileText().isBlank()) {
                        templateBlock
                                        .append("### TÀI LIỆU ĐÍNH KÈM\n")
                                        .append(context.uploadedFileText().trim())
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
                                templateBlock.toString().trim());
        }

        private void appendIfNotBlank(
                        StringBuilder builder,
                        String value) {
                if (value != null && !value.isBlank()) {
                        builder
                                        .append(value.trim())
                                        .append("\n\n");
                }
        }

        private String buildRefineUserContent(
                        String editorText,
                        String userText) {

                return """
                                Nội dung hiện tại:
                                %s

                                Yêu cầu người dùng:
                                %s
                                """.formatted(
                                editorText.trim(),
                                userText.trim());
        }

        // =========================================================
        // COMMON CONTEXT
        // =========================================================

        /*
         * Build AiPromptContext with nội dung các file trong fileId
         */
        private AiPromptContext buildAiPromptContext(
                        ChatSession session,
                        User user,
                        DocumentFormDTO formData,
                        List<String> fileIds) {

                if (session == null) {
                        throw new IllegalArgumentException("Chat session must not be null");
                }

                AiTemplateContent templateContent = aiTemplateService.resolve(
                                session,
                                formData.getLoaiVanBan(),
                                formData.getTenVanBan());

                // Build uploadedFileText from fileIds
                String uploadedFileText = buildUploadedFileText(
                                session,
                                user.getUserId(),
                                fileIds);

                return new AiPromptContext(
                                templateContent,
                                promptTemplate,
                                uploadedFileText);
        }

        // Lấy nội dung của các file đã được upload dựa trên fileIds
        private String buildUploadedFileText(
                        ChatSession session,
                        String userId,
                        List<String> fileIds) {
                if (fileIds == null || fileIds.isEmpty()) {
                        return "";
                }

                List<FileMetadata> sessionFiles = chatSessionFileService.getFilesBySession(session);

                StringBuilder textBuilder = new StringBuilder();

                for (String rawFileId : fileIds) {
                        if (rawFileId == null || rawFileId.isBlank()) {
                                continue;
                        }

                        String normalzedFileId = rawFileId.trim();

                        if (normalzedFileId.startsWith("file_")) {
                                normalzedFileId = normalzedFileId.substring("file_".length());
                        }

                        final String fileId = normalzedFileId;
                        FileMetadata selectedFile = sessionFiles.stream()
                                        .filter(file -> fileId.equals(file.getFileId()))
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "File không thuộc session hiện tại: "
                                                                        + rawFileId));

                        String text = fileTextService.getNormalizedText(
                                        selectedFile.getFileId(),
                                        userId);

                        if (text == null || text.isBlank()) {
                                continue;
                        }

                        String category = selectedFile.getCategory() != null
                                        ? selectedFile.getCategory().name()
                                        : "UNKNOWN";

                        textBuilder
                                        .append("[UPLOADED_FILE]\n")
                                        .append("fileName: ")
                                        .append(selectedFile.getFileName())
                                        .append("\n")
                                        .append("category: ")
                                        .append(category)
                                        .append("\n\n")
                                        .append(text.trim())
                                        .append("\n\n");
                }

                return textBuilder.toString().trim();
        }

        /**
         * Refine chỉ cần FormData để xác định template.
         * Không dùng FormData làm nội dung chính của prompt.
         */
        private DocumentFormDTO loadFormDataForContext(
                        ChatSession session,
                        User user) {

                String wizardStateJson = draftFileService.getFormDataBySession(
                                session.getSessionUuid(),
                                user.getUserId());

                if (wizardStateJson == null
                                || wizardStateJson.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Draft không có FormData để xác định template");
                }

                try {

                        DocumentFormDTO formData = objectMapper.readValue(
                                        wizardStateJson,
                                        DocumentFormDTO.class);

                        validateFormData(formData);

                        return formData;

                } catch (IllegalArgumentException e) {

                        throw e;

                } catch (Exception e) {

                        throw new IllegalArgumentException(
                                        "wizardStateJson của draft không chứa FormData JSON hợp lệ",
                                        e);
                }
        }

        // =========================================================
        // COMMON AI EXECUTION
        // =========================================================

        private AiProviderResultDTO executeAI(
                        ChatSession session,
                        User user,
                        ModelRoute modelRoute,
                        List<OpenRouterMessageDTO> messages,
                        String userText,
                        String templateFileName) {

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

                String safeTemplateFileName = templateFileName == null
                                || templateFileName.isBlank()
                                                ? "UNKNOWN_TEMPLATE"
                                                : templateFileName;

                holdReq.setDescription(
                                "AI HOLD - session "
                                                + session.getSessionUuid()
                                                + " - template "
                                                + safeTemplateFileName);

                holdReq.setSessionUuid(session.getSessionUuid());
                holdReq.setModel(modelRoute.primaryModel());

                CreditTransaction holdTx = creditService.recordHoldTransactionWithSnapshot(user.getUserId(),
                                session.getSessionId(), holdReq);

                // 12. Tạo OpenRouter request
                OpenRouterRequestDTO request = new OpenRouterRequestDTO(
                                modelRoute.primaryModel(),
                                messages,
                                modelRoute.temperature(),
                                modelRoute.maxTokens(),
                                false);

                try {
                        String requestJson = objectMapper.writerWithDefaultPrettyPrinter()
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
                        } catch (Exception ignore) {
                        }
                        throw ex;
                }

                if (aiResult == null) {
                        // provider returned null -> refund and fail
                        try {
                                creditService.refundHold(holdTx.getTransactionId());
                        } catch (Exception ignore) {
                        }
                        throw new IllegalStateException("AI không trả về kết quả");
                }

                // 14. On success: compute pricing and settle
                long promptTokens = aiResult.promptTokens();
                long completionTokens = aiResult.completionTokens();
                long totalTokens = aiResult.totalTokens();

                double baseCredit = creditPricingService.calculateBaseCredit(promptTokens, completionTokens,
                                outputWeight);
                double actualCredit = creditPricingService.calculateActualCredit(baseCredit, creditRate);

                // settle (updates same transaction, deducts actual credit and sets refunded)
                creditService.settleTransaction(
                                holdTx.getTransactionId(),
                                promptTokens,
                                completionTokens,
                                totalTokens,
                                actualCredit,
                                aiResult.model());

                // save model into transaction if needed (ensure settleTransaction handles model
                // persist)
                // Chuyển session sang EDITING (save result)
                if (aiResult.content() != null
                                && !aiResult.content().isBlank()) {

                        try {

                                chatSessionService.saveAiResult(
                                                session.getSessionId(),
                                                user.getUserId(),
                                                aiResult.content());

                                chatSessionService.appendUserMessage(
                                                session.getSessionUuid(),
                                                user.getUserId(),
                                                userText);

                        } catch (Exception e) {

                                System.err.println(
                                                "Không thể lưu kết quả AI vào session "
                                                                + session.getSessionId()
                                                                + ": "
                                                                + e.getMessage());
                        }
                }

                SessionStatus sessionStatus = chatSessionService.markEditing(
                                session.getSessionId(),
                                user.getUserId());

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
                        AiCompletionRequestDTO input) {

                if (input == null) {
                        throw new IllegalArgumentException(
                                        "Dữ liệu yêu cầu AI không được để trống");
                }

                if (input.getSessionUuid() == null
                                || input.getSessionUuid().isBlank()) {

                        throw new IllegalArgumentException(
                                        "sessionUuid không được để trống");
                }
        }

        private void validateRefineInput(
                        AiCompletionRequestDTO input) {

                validateInput(input);

                if (input.getUserText() == null
                                || input.getUserText().isBlank()) {

                        throw new IllegalArgumentException(
                                        "userText không được để trống");
                }
        }

        private void validateFormData(
                        DocumentFormDTO formData) {

                if (formData == null) {
                        throw new IllegalArgumentException(
                                        "FormData trong draft không được để trống");
                }

                if (formData.getLoaiVanBan() == null
                                || formData.getLoaiVanBan().isBlank()) {

                        throw new IllegalArgumentException(
                                        "loaiVanBan không được để trống");
                }

                if (formData.getTenVanBan() == null
                                || formData.getTenVanBan().isBlank()) {

                        throw new IllegalArgumentException(
                                        "tenVanBan không được để trống");
                }
        }

        // =========================================================
        // FILE CONTEXT
        // =========================================================

        // Lấy nội dung của một file đã được upload dựa trên fileId

        // Xử lý PDF đã quét và trích xuất định nghĩa template từ AI
        public ScanTemplateResponseDTO processScannedPdf(
                        User user,
                        String sessionUuid,
                        List<MultipartFile> images,
                        boolean isLastBatch) throws IOException {

                if (sessionUuid == null || sessionUuid.isBlank()) {
                        throw new IllegalArgumentException(
                                        "sessionUuid không được để trống");
                }

                if (images == null || images.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Phải gửi ít nhất một ảnh");
                }

                if (images.size() > 5) {
                        throw new IllegalArgumentException(
                                        "Mỗi request tối đa 5 ảnh");
                }

                ChatSession session = chatSessionRepository
                                .findBySessionUuidAndUser_UserId(
                                                sessionUuid,
                                                user.getUserId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Session không tồn tại"));

                if (session.getStatus() == ChatSession.SessionStatus.CLOSED) {
                        throw new IllegalArgumentException(
                                        "Session đã bị đóng");
                }

                /*
                 * Tạm thời xử lý trực tiếp batch hiện tại.
                 * Khi isLastBatch=true, frontend phải gửi toàn bộ ảnh cần phân tích
                 * hoặc backend phải có storage để lưu các batch trước đó.
                 */

                ModelRoute modelRoute = packageRoutingService.resolveRoute(
                                user,
                                null);

                OpenRouterVisionRequestDTO request = buildScannedPdfPrompt(
                                images,
                                modelRoute);

                AiProviderResultDTO aiResult = openRouterClient.completeVision(request);

                String templateDefinition = extractTemplateDefinition(aiResult.content());

                session.setTemplateDefinitionJson(templateDefinition);
                chatSessionRepository.save(session);

                return ScanTemplateResponseDTO.builder()
                                .sessionUuid(sessionUuid)
                                .completed(true)
                                .templateDefinition(templateDefinition)
                                .aiResult(aiResult)
                                .build();
        }

        private OpenRouterVisionRequestDTO buildScannedPdfPrompt(
                        List<MultipartFile> images,
                        ModelRoute modelRoute) throws IOException {

                List<Object> userContent = new ArrayList<>();

                userContent.add(Map.of(
                                "type", "text",
                                "text", "Phân tích các ảnh PDF scan này."));

                for (MultipartFile image : images) {
                        if (image == null || image.isEmpty()) {
                                throw new IllegalArgumentException(
                                                "Ảnh PDF không được để trống");
                        }

                        String contentType = image.getContentType();

                        if (contentType == null
                                        || !contentType.startsWith("image/")) {
                                throw new IllegalArgumentException(
                                                "File gửi lên phải là ảnh");
                        }

                        String base64 = Base64.getEncoder()
                                        .encodeToString(image.getBytes());

                        userContent.add(Map.of(
                                        "type", "image_url",
                                        "image_url", Map.of(
                                                        "url", "data:"
                                                                        + contentType
                                                                        + ";base64,"
                                                                        + base64)));
                }

                return new OpenRouterVisionRequestDTO(
                                modelRoute.primaryModel(),
                                List.of(
                                                new OpenRouterVisionMessageDTO(
                                                                "system",
                                                                scanTemplatePrompt),
                                                new OpenRouterVisionMessageDTO(
                                                                "user",
                                                                userContent)),
                                modelRoute.temperature(),
                                modelRoute.maxTokens(),
                                false);
        }

        private String extractTemplateDefinition(
                        String aiContent) {
                if (aiContent == null || aiContent.isBlank()) {
                        throw new IllegalStateException(
                                        "AI không trả về TemplateDefinition");
                }

                String json = aiContent.trim();

                if (json.startsWith("```")) {
                        json = json
                                        .replaceFirst("^```json\\s*", "")
                                        .replaceFirst("^```\\s*", "")
                                        .replaceFirst("\\s*```$", "")
                                        .trim();
                }

                try {
                        objectMapper.readTree(json);
                        return json;
                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "AI trả về TemplateDefinition không phải JSON hợp lệ",
                                        exception);
                }
        }

        // Phân tích raw text và trích xuất TemplateDefinition từ AI
        public String extractTemplateFromRawText(
                        User user,
                        String rawText) {
                if (rawText == null || rawText.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Không có raw_text để phân tích template");
                }

                ModelRoute route = packageRoutingService.resolveRoute(user, null);

                String userContent = """
                                Hãy phân tích toàn bộ văn bản dưới đây theo prompt hệ thống.

                                === RAW TEXT ===
                                %s
                                === END RAW TEXT ===
                                """.formatted(rawText);

                OpenRouterRequestDTO request = new OpenRouterRequestDTO(
                                route.primaryModel(),
                                List.of(
                                                new OpenRouterMessageDTO(
                                                                "system",
                                                                scanTemplatePrompt),
                                                new OpenRouterMessageDTO(
                                                                "user",
                                                                userContent)),
                                route.temperature(),
                                route.maxTokens(),
                                false);

                AiProviderResultDTO result = openRouterClient.complete(request);

                log.info(
                                "Template extraction result, file content length={}, ai content={}",
                                rawText.length(),
                                result != null ? result.content() : null);

                return extractTemplateDefinition(result.content());
        }

        // Validate số lượng fileId được gửi lên cho AI, dựa trên gói của user
        private void validateAiFileIdsLimit(
                        List<String> fileIds,
                        User user) {
                if (fileIds == null || fileIds.isEmpty()) {
                        return;
                }

                AdminPackageConfig packageConfig = user.getAdminPackageConfig();

                if (packageConfig == null) {
                        packageConfig = adminService.getPackageConfig("FREE");
                }

                int maxFiles = packageConfig != null
                                && packageConfig.getMaxFilesPerCall() != null
                                                ? packageConfig.getMaxFilesPerCall()
                                                : 5;

                if (fileIds.size() > maxFiles) {
                        throw new IllegalArgumentException(
                                        "Số lượng file context vượt quá giới hạn "
                                                        + maxFiles
                                                        + " file");
                }

                long distinctCount = fileIds.stream()
                                .filter(Objects::nonNull)
                                .distinct()
                                .count();

                if (distinctCount != fileIds.size()) {
                        throw new IllegalArgumentException(
                                        "Danh sách fileId không được trùng nhau");
                }
        }

}