package com.saasai.service;

import com.saasai.extractor.ExtractResult;
import com.saasai.extractor.FileExtractor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Service
public class FileExtractionService {

    private final List<FileExtractor> extractors;

    public FileExtractionService(List<FileExtractor> extractors) {
        this.extractors = extractors;
    }

    // Extracts the content of a file based on its extension using the appropriate extractor.
    // Throws an exception if the file path is null or if no extractor supports the file's extension.
    // Returns an ExtractResult containing the extracted content and metadata.
    public ExtractResult extract(Path filePath, String originalFileName) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("Đường dẫn file không được để trống");
        }

        String extension = getExtension(originalFileName);

        FileExtractor extractor = extractors.stream()
                .filter(item -> item.supports(extension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Chưa có extractor hỗ trợ định dạng: " + extension
                ));

        return extractor.extract(filePath);
    }


    // Helper method to get the file extension from the original file name.
    // Throws an exception if the file name is null, blank, or does not contain a period.
    private String getExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            throw new IllegalArgumentException("Tên file hoặc định dạng file không hợp lệ");
        }

        return fileName.substring(fileName.lastIndexOf('.') + 1)
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}