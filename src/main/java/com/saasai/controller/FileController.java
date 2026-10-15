package com.saasai.controller;

import com.saasai.dto.FileMetadataResponseDTO;
import com.saasai.dto.ScanOcrResponseDTO;
import com.saasai.dto.SessionFileOptionDTO;
import com.saasai.dto.UploadConfigResponseDTO;
import com.saasai.dto.ScanAiTextResponseDTO;



import java.util.List;
import com.saasai.service.ChatSessionFileService;
import com.saasai.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import com.saasai.service.FileTextService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files") // Hoặc endpoint cha tùy cấu hình urls.ts của ông
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileTextService fileTextService;

    @Autowired
    private ChatSessionFileService chatSessionFileService;

    @GetMapping
    public ResponseEntity<List<SessionFileOptionDTO>> getFilesBySession(
            @RequestParam("sessionUuid") String sessionUuid
    ) {
        String userId =
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        List<SessionFileOptionDTO> files =
                chatSessionFileService.getFileOptionsBySession(
                        sessionUuid,
                        userId
                );

        return ResponseEntity.ok(files);
    }


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponseDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sessionUuid") String sessionUuid,
            @RequestParam(value = "fieldCode", required = false) String fieldCode
    ) throws IOException {
        
        // Gọi thẳng Service xử lý lưu cục bộ và lưu DB mà ông đã viết
        FileMetadataResponseDTO response =
            fileService.uploadFile(file, category, sessionUuid, fieldCode);
        
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{fileId}/normalized-text")
        public ResponseEntity<Map<String, String>> getNormalizedText(@PathVariable String fileId) {
            String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String normalizedText = fileTextService.getNormalizedText(fileId, userId);
            return ResponseEntity.ok(Map.of("fileId", fileId, "normalizedText", normalizedText));
        }


        @PostMapping(value = "/scan-documents/ocr",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ScanOcrResponseDTO> processScanOcr(
                @RequestParam("fileId") String fileId,
                @RequestParam("sessionUuid") String sessionUuid,
                @RequestParam("images") List<MultipartFile> images,
                @RequestParam("pageIndexes") List<Integer> pageIndexes,
                @RequestParam("isLastBatch") boolean isLastBatch
        ) throws IOException {
        ScanOcrResponseDTO response =
                fileService.processScanOcr(
                        fileId,
                        sessionUuid,
                        images,
                        pageIndexes,
                        isLastBatch
                );

        return ResponseEntity.ok(response);
        }

        // Endpoint xử lý OCR cho các tài liệu được quét từ AI
        @PostMapping(
                value = "/scan-documents/ai-text",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE
        )
        public ResponseEntity<ScanAiTextResponseDTO> processScanAiText(
                @RequestParam("fileId") String fileId,
                @RequestParam("sessionUuid") String sessionUuid,
                @RequestParam("images") List<MultipartFile> images,
                @RequestParam("pageIndexes") List<Integer> pageIndexes,
                @RequestParam("isLastBatch") boolean isLastBatch
        ) throws IOException {

        return ResponseEntity.ok(
                fileService.processScanAiText(
                        fileId,
                        sessionUuid,
                        images,
                        pageIndexes,
                        isLastBatch
                )
        );
        }

        // Endpoint để lấy cấu hình upload dựa trên gói của người dùng hiện tại
        @GetMapping("/upload/config")
        public ResponseEntity<UploadConfigResponseDTO> getUploadConfig() {
        return ResponseEntity.ok(fileService.getUploadConfig());
        }
}