package com.saasai.controller;

import com.saasai.dto.DraftFinalizeRequestDTO;
import com.saasai.dto.DraftSaveRequestDTO;
import com.saasai.dto.DraftStateDTO;
import com.saasai.service.DraftFileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/drafts")
public class DraftController {

    private final DraftFileService draftFileService;

    public DraftController(DraftFileService draftFileService) {
        this.draftFileService = draftFileService;
    }

    @PutMapping
    public ResponseEntity<DraftStateDTO> saveDraft(
            @RequestBody DraftSaveRequestDTO request
    ) {
        DraftStateDTO state = draftFileService.saveDraftState(
                request.getSessionUuid(),
                currentUserId(),
                request.getEditorText(),
                request.getFormData(),
                request.getFieldCode()
        );

        return ResponseEntity.ok(state);
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<DraftStateDTO> loadDraft(
            @PathVariable String draftId
    ) {
        return ResponseEntity.ok(
                draftFileService.loadDraftState(draftId, currentUserId())
        );
    }

    @PostMapping("/{draftId}/finalize")
    public ResponseEntity<Map<String, String>> finalizeDraft(
            @PathVariable String draftId,
            @RequestBody(required = false) DraftFinalizeRequestDTO request
    ) {
        String mergedPrompt = draftFileService.finalizeDraftAndBuildPrompt(
                draftId,
                currentUserId()
        );

        String instruction = request != null ? request.getInstruction() : null;

        if (instruction != null && !instruction.isBlank()) {
            mergedPrompt = instruction.trim() + "\n\n" + mergedPrompt;
        }

        return ResponseEntity.ok(Map.of(
                "draftId", draftId,
                "mergedPrompt", mergedPrompt
        ));
    }

    @DeleteMapping("/{draftId}")
    public ResponseEntity<Void> deleteDraft(@PathVariable String draftId) {
        draftFileService.deleteDraft(draftId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    private String currentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException("Không tìm thấy Authentication");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof String userId && !userId.isBlank()) {
            return userId;
        }

        throw new IllegalStateException(
                "Không tìm thấy userId trong SecurityContext"
        );
    }
}