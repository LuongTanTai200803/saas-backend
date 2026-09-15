package com.saasai.controller;

import com.saasai.dto.FileMetadataResponseDTO;
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponseDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sessionUuid", required = false) String sessionUuid,
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

}