package com.saasai.feature.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.saasai.feature.ai.AiCompletionRequestDTO;
import com.saasai.feature.ai.AiProviderResultDTO;
import com.saasai.feature.ai.ApiResponseDTO;
import com.saasai.dto.ExportRequestDTO;
import com.saasai.entity.User;
import com.saasai.service.UserService;

import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin
public class AiController {

        @Autowired
        private AiService aiService;

        @Autowired
        private UserService userService;

        @Autowired
        private AiResourceService aiResourceService;

        @Autowired
        private AiTemplateService aiTemplateService;

        private AiCompletionService aiCompletionService;

        public AiController(AiCompletionService aiCompletionService) {
                this.aiCompletionService = aiCompletionService;
        }

        @PostMapping("/completions")
        public ResponseEntity<ApiResponseDTO<AiProviderResultDTO>> complete(
                        @RequestBody AiCompletionRequestDTO request) {

                String userId = (String) SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

                User user = userService.getUserById(userId);

                AiProviderResultDTO result = aiCompletionService.complete(user, request);

                return ResponseEntity.ok(
                                ApiResponseDTO.success(result));
        }

        @PostMapping("/workspace/refine")
        public ResponseEntity<ApiResponseDTO<AiProviderResultDTO>> refineWorkspace(
                        @RequestBody AiCompletionRequestDTO request) {
                String userId = (String) SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

                User user = userService.getUserById(userId);

                AiProviderResultDTO result = aiCompletionService.refineWorkspace(user, request);

                return ResponseEntity.ok(ApiResponseDTO.success(result));
        }

        @PostMapping(value = "/scanned-pdf/template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponseDTO<ScanTemplateResponseDTO>> processScannedPdf(
                        @RequestParam("sessionUuid") String sessionUuid,

                        @RequestParam("images") List<MultipartFile> images,

                        @RequestParam("isLastBatch") boolean isLastBatch) throws IOException {

                String userId = (String) SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

                User user = userService.getUserById(userId);

                ScanTemplateResponseDTO result = aiCompletionService.processScannedPdf(
                                user,
                                sessionUuid,
                                images,
                                isLastBatch);

                return ResponseEntity.ok(
                                ApiResponseDTO.success(result));
        }

        @PostMapping("/proofread")
        public ResponseEntity<ApiResponseDTO<ProofreadResponseDTO>> proofread(
                        @Valid @RequestBody ProofreadRequestDTO request) {

                String userId = (String) SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

                User user = userService.getUserById(userId);
                ProofreadResponseDTO result = aiCompletionService.proofread(user, request);

                return ResponseEntity.ok(ApiResponseDTO.success(result));
        }

        @GetMapping("/templates")
        public ResponseEntity<ApiResponseDTO<AiResourceService.TemplateCatalog>> getTemplates(
                        @RequestParam String topicCode) {

                return ResponseEntity.ok(
                                ApiResponseDTO.success(
                                                aiResourceService.listTemplatePreviews(topicCode)));
        }

        @PostMapping("/templates/select")
        public ResponseEntity<ApiResponseDTO<Map<String, String>>> selectSystemTemplate(
                        @Valid @RequestBody SelectSystemTemplateRequestDTO request) {

                String userId = (String) SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

                Map<String, String> result = aiTemplateService.selectSystemTemplate(
                                userId,
                                request.sessionUuid(),
                                request.topicCode(),
                                request.templateCode());

                return ResponseEntity.ok(ApiResponseDTO.success(result));
        }
}