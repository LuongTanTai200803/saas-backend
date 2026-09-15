package com.saasai.extractor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ExtractResult {

    /**
     * Nội dung thô lấy trực tiếp từ file.
     * Chưa thực hiện normalize ở bước này.
     */
    private final String rawText;

    /**
     * Metadata tùy từng loại file.
     *
     * Ví dụ:
     * DOCX: paragraphCount
     * XLSX: sheetCount, rowCount
     * TXT: encoding
     */
    @Builder.Default
    private final Map<String, Object> metadata = Collections.emptyMap();
}