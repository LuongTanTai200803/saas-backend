package com.saasai.extractor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class PdfFileExtractor implements FileExtractor {

    @Override
    public boolean supports(String extension) {
        return extension != null
                && "pdf".equalsIgnoreCase(extension.trim());
    }

    @Override
    public ExtractResult extract(Path filePath) throws IOException {
        validatePath(filePath);

        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException(
                        "Không thể đọc PDF được mã hóa"
                );
            }

            PDFTextStripper textStripper = new PDFTextStripper();
            String rawText = textStripper.getText(document);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileType", "PDF");
            metadata.put("pageCount", document.getNumberOfPages());

            return ExtractResult.builder()
                    .rawText(rawText)
                    .metadata(metadata)
                    .build();
        }
    }

    private void validatePath(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException(
                    "Đường dẫn file PDF không được để trống"
            );
        }

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "File PDF không tồn tại: " + filePath
            );
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException(
                    "Đường dẫn PDF không phải file hợp lệ: " + filePath
            );
        }
    }
}
