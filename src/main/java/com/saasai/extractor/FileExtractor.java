package com.saasai.extractor;

import java.io.IOException;
import java.nio.file.Path;

public interface FileExtractor {

    /**
     * Kiểm tra extractor có hỗ trợ extension này không.
     *
     * Ví dụ:
     * docx
     * xlsx
     * txt
     * 
     * PdfFileExtractor
        DocFileExtractor
        CsvFileExtractor
        ImageOcrExtractor
     */
    boolean supports(String extension);

    /**
     * Bóc nội dung thô từ file đã lưu trên ổ cứng.
     */
    ExtractResult extract(Path filePath) throws IOException;
}