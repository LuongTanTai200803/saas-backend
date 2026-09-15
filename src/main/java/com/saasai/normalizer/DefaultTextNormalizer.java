package com.saasai.normalizer;

import org.springframework.stereotype.Component;

@Component
public class DefaultTextNormalizer implements TextNormalizer {

    @Override
    public String normalize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String normalized = rawText
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ');

        StringBuilder result = new StringBuilder();
        String[] lines = normalized.split("\n", -1);
        int consecutiveBlankLines = 0;

        for (String line : lines) {
            String cleanedLine = normalizeLine(line);

            if (cleanedLine.isBlank()) {
                consecutiveBlankLines++;

                // Chỉ giữ tối đa một dòng trống liên tiếp.
                if (consecutiveBlankLines <= 1) {
                    result.append('\n');
                }

                continue;
            }

            consecutiveBlankLines = 0;
            result.append(cleanedLine).append('\n');
        }

        return result.toString().trim();
    }

    private String normalizeLine(String line) {
        if (line == null) {
            return "";
        }

        // Giữ tab để không phá cấu trúc hàng/cột của Excel.
        String[] tabSegments = line.split("\t", -1);
        StringBuilder cleanedLine = new StringBuilder();

        for (int i = 0; i < tabSegments.length; i++) {
            String segment = tabSegments[i]
                    .trim()
                    .replaceAll("[ \\f\\v]+", " ");

            if (i > 0) {
                cleanedLine.append('\t');
            }

            cleanedLine.append(segment);
        }

        return cleanedLine.toString().stripTrailing();
    }
}