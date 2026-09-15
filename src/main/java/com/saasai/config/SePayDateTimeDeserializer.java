package com.saasai.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class SePayDateTimeDeserializer
        extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter SPACE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(
            JsonParser parser,
            DeserializationContext context
    ) throws IOException {

        String value = parser.getText();

        if (value == null || value.isBlank()) {
            return null;
        }

        // Nhận dạng: 2026-09-09T12:34:56+07:00
        if (value.contains("+") || value.endsWith("Z")) {
            return OffsetDateTime.parse(value)
                    .toLocalDateTime();
        }

        // Nhận dạng: 2026-09-09T12:34:56
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }

        // Nhận dạng cũ: 2026-09-09 12:34:56
        return LocalDateTime.parse(value, SPACE_FORMAT);
    }
}