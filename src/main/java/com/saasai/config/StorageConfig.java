package com.saasai.config;

import com.saasai.storage.LocalStorageServiceImpl;
import com.saasai.storage.S3StorageServiceImpl;
import com.saasai.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Value("${storage.type:local}")
    private String storageType;

    @Value("${storage.local.base-path:uploads/}")
    private String localBasePath;

    @Bean
    public StorageService storageService() {
        if ("s3".equalsIgnoreCase(storageType) || "r2".equalsIgnoreCase(storageType)) {
            return new S3StorageServiceImpl();
        }
        return new LocalStorageServiceImpl(localBasePath);
    }
}
