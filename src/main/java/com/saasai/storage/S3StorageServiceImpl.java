package com.saasai.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageServiceImpl implements StorageService {

    public S3StorageServiceImpl() {
        // TODO: implement actual S3/R2 storage provider initialization.
    }

    @Override
    public String storeFile(MultipartFile file, String fileName) throws IOException {
        throw new UnsupportedOperationException("S3 storage is not implemented yet");
    }

    @Override
    public byte[] loadFile(String fileName) throws IOException {
        throw new UnsupportedOperationException("S3 storage is not implemented yet");
    }

    @Override
    public boolean exists(String fileName) throws IOException {
        throw new UnsupportedOperationException("S3 storage is not implemented yet");
    }

    @Override
    public java.nio.file.Path getFilePath(String storedFileName) {
        throw new UnsupportedOperationException("S3 storage is not implemented yet");
    }
}
