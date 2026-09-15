package com.saasai.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saasai.dto.ExportRequestDTO;
import com.saasai.dto.ExportResult;
import com.saasai.service.ExportService;

import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestBody ExportRequestDTO request
    ) throws Exception {
        ExportResult result = exportService.export(
                request.getSessionUuid(),
                request.getExportFormat(),
                currentUserId()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.fileName() + "\""
                )
                .contentType(result.mediaType())
                .body(result.bytes());
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
