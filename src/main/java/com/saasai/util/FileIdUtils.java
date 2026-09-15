package com.saasai.util;

public final class FileIdUtils {

    private static final String PREFIX = "file_";

    private FileIdUtils() {
    }

    public static String toExternal(String internalId) {
        if (internalId == null || internalId.isBlank()) {
            throw new IllegalArgumentException("internalId không được để trống");
        }

        return PREFIX + internalId;
    }

    public static String toInternal(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId không được để trống");
        }

        String normalized = externalId.trim();

        if (normalized.startsWith(PREFIX)) {
            normalized = normalized.substring(PREFIX.length());
        }

        return normalized;
    }
}