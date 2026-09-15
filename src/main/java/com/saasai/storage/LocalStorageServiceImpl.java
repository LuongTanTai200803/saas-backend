package com.saasai.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageServiceImpl implements StorageService {
    private final Path basePath;

    // 🎯 SỬA LẠI TẠI ĐÂY: Gài @Value để Spring bốc cấu hình chính chủ từ file .properties ra
    public LocalStorageServiceImpl(@Value("${storage.local.base-path:uploads/}") String basePath) {
        this.basePath = Paths.get(StringUtils.hasText(basePath) ? basePath : "uploads/");
    }

    @Override
    public String storeFile(MultipartFile file, String fileName) throws IOException {
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
        }
        Path target = basePath.resolve(fileName);
        Files.write(target, file.getBytes());
        return target.toString();
    }

    @Override
    public byte[] loadFile(String fileName) throws IOException {
        return Files.readAllBytes(basePath.resolve(fileName));
    }

    @Override
    public boolean exists(String fileName) throws IOException {
        return Files.exists(basePath.resolve(fileName));
    }
    
    @Override
    public Path getFilePath(String storedFileName) {
        return basePath.resolve(storedFileName);
    }
}