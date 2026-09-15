package com.saasai.normalizer;

public interface TextNormalizer {

    /**
     * Chuẩn hóa nội dung text thô sau khi bóc từ file.
     *
     * @param rawText nội dung thô từ extractor
     * @return nội dung đã được chuẩn hóa
     */
    String normalize(String rawText);
}