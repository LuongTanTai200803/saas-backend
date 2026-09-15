package com.saasai.extractor;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class TxtFileExtractor implements FileExtractor {

    @Override
    public boolean supports(String extension) {
        return extension != null
                && "txt".equalsIgnoreCase(extension.trim());
    }

    @Override
    public ExtractResult extract(Path filePath) throws IOException {
        validatePath(filePath);

        String rawText = Files.readString(
                filePath,
                StandardCharsets.UTF_8
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileType", "TXT");
        metadata.put("encoding", StandardCharsets.UTF_8.name());

        return ExtractResult.builder()
                .rawText(rawText)
                .metadata(metadata)
                .build();
    }

    private void validatePath(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException(
                    "Đường dẫn file TXT không được để trống"
            );
        }

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "File TXT không tồn tại: " + filePath
            );
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException(
                    "Đường dẫn TXT không phải file hợp lệ: " + filePath
            );
        }
    }
}