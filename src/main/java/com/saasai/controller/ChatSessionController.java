package com.saasai.controller;

import com.saasai.feature.ai.ApiResponseDTO;
import com.saasai.dto.ChatSessionCreateRequestDTO;
import com.saasai.dto.ChatSessionDTO;
import com.saasai.dto.ChatSessionWorkspaceDTO;
import com.saasai.dto.EditorContentUpdateDTO;
import com.saasai.dto.UpdateSessionNameRequestDTO;
import com.saasai.entity.ChatSession;
import com.saasai.service.ChatSessionService;
import com.saasai.service.DraftFileService;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat-sessions")
@CrossOrigin
public class ChatSessionController {

        private final ChatSessionService chatSessionService;
        private final DraftFileService draftFileService;

        public ChatSessionController(
                ChatSessionService chatSessionService,
                DraftFileService draftFileService
        ) {
                this.chatSessionService = chatSessionService;
                this.draftFileService = draftFileService;
        }

        @PostMapping
        public ResponseEntity<ApiResponseDTO<ChatSessionDTO>> createSession(
                @RequestBody ChatSessionCreateRequestDTO request
        ) {
                ChatSessionDTO session = chatSessionService.createSession(
                        currentEmail(),
                        request
                );
                return ResponseEntity.status(HttpStatus.CREATED).body(
                        ApiResponseDTO.success(session)
                );
        }

        @GetMapping
        public ResponseEntity<ApiResponseDTO<List<ChatSessionDTO>>> getSessions(
                @RequestParam Integer assistantId
        ) {
        List<ChatSessionDTO> sessions =
                chatSessionService.getSessionsByUser(
                        currentUserId(),
                        assistantId
                );

        return ResponseEntity.ok(
                ApiResponseDTO.success(sessions)
        );
        }
        
        @DeleteMapping("/{sessionUuid}")
        public ResponseEntity<ApiResponseDTO<Object>> deleteSession(
                        @PathVariable String sessionUuid
        ) {
                chatSessionService.deleteSession(sessionUuid, currentUserId());
                        return ResponseEntity.ok(ApiResponseDTO.builder()
                                .success(true)
                                .message("Đã xóa phiên trò chuyện thành công")
                                .statusCode(200)
                                .build());
                }

        @PutMapping("/{sessionUuid}/name")
                public ResponseEntity<ApiResponseDTO<ChatSessionDTO>> updateSessionName(
                        @PathVariable String sessionUuid,
                        @RequestBody UpdateSessionNameRequestDTO request
                ) {
                ChatSessionDTO updated = chatSessionService.updateSessionName(
                        currentUserId(),
                        sessionUuid,
                        request.getSessionName()
                );

        return ResponseEntity.ok(ApiResponseDTO.success(updated));
        }

        @PutMapping("/{sessionUuid}/editor")
        public ResponseEntity<ApiResponseDTO<Object>> updateEditorContent(
                @PathVariable String sessionUuid,
                @RequestBody EditorContentUpdateDTO request
        ) {
                chatSessionService.updateEditorContent(
                        sessionUuid,
                        currentUserId(),
                        request.getHtmlContent()
                );

                return ResponseEntity.ok(ApiResponseDTO.builder()
                        .success(true)
                        .message("Đã lưu bản nháp văn bản thành công")
                        .statusCode(200)
                        .build());
        }

        @GetMapping("/{sessionUuid}/editor")
        public ResponseEntity<String> loadEditorContent(
                @PathVariable String sessionUuid
        ) {
                // Service nhận đúng thứ tự: userId, sessionUuid.
                String content = chatSessionService.loadEditorContent(
                        currentUserId(),
                        sessionUuid
                );
                return ResponseEntity.ok(content);
        }

        @GetMapping("/{sessionUuid}/export")
        public ResponseEntity<byte[]> exportDraft(
                @PathVariable String sessionUuid
        ) {
                byte[] fileBytes = chatSessionService.exportCurrentDraft(
                        currentUserId(),
                        sessionUuid
                );

                String shortId = sessionUuid.length() > 8
                        ? sessionUuid.substring(0, 8)
                        : sessionUuid;

                return ResponseEntity.ok()
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"draft-" + shortId + ".txt\""
                        )
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(fileBytes);
        }

        @PostMapping("/{sessionUuid}/finalize")
        public ResponseEntity<String> finalizeDocument(
                @PathVariable String sessionUuid
        ) {
                // Quy ước tạm: draftId chính là sessionUuid.
                String mergedPrompt = draftFileService.finalizeDraftAndBuildPrompt(
                        sessionUuid,
                        currentUserId()
                );
                return ResponseEntity.ok(mergedPrompt);
        }

        private String currentUserId() {
                Object principal = SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

                if (!(principal instanceof String userId) || userId.isBlank()) {
                throw new IllegalStateException("Không tìm thấy userId trong SecurityContext");
                }
                return userId;
        }

        private String currentEmail() {
                Object details = SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getDetails();

                if (!(details instanceof String email) || email.isBlank()) {
                throw new IllegalStateException("Không tìm thấy email trong SecurityContext");
                }
                return email;
        }

        @GetMapping("/{sessionUuid}/workspace")
                public ResponseEntity<ApiResponseDTO<ChatSessionWorkspaceDTO>> getWorkspace(
                        @PathVariable String sessionUuid
                ) {
                        ChatSessionWorkspaceDTO workspace =
                                chatSessionService.getWorkspace(currentUserId(), sessionUuid);

                        return ResponseEntity.ok(ApiResponseDTO.success(workspace));
                }


}