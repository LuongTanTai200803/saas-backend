package com.saasai.extractor;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class DocxFileExtractor implements FileExtractor {

    @Override
    public boolean supports(String extension) {
        return extension != null
                && "docx".equalsIgnoreCase(extension.trim());
    }

    @Override
    public ExtractResult extract(Path filePath) throws IOException {
        validatePath(filePath);

        StringBuilder rawText = new StringBuilder();
        int paragraphCount = 0;

        try (
                InputStream inputStream =
                        new BufferedInputStream(Files.newInputStream(filePath));

                XWPFDocument document =
                        new XWPFDocument(inputStream)
        ) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = paragraph.getText();

                if (paragraphText == null) {
                    continue;
                }

                rawText.append(paragraphText)
                        .append(System.lineSeparator());

                paragraphCount++;
            }
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileType", "DOCX");
        metadata.put("paragraphCount", paragraphCount);

        return ExtractResult.builder()
                .rawText(rawText.toString())
                .metadata(metadata)
                .build();
    }

    private void validatePath(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException(
                    "Đường dẫn file DOCX không được để trống"
            );
        }

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "File DOCX không tồn tại: " + filePath
            );
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException(
                    "Đường dẫn DOCX không phải file hợp lệ: " + filePath
            );
        }
    }
}