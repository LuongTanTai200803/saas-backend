package com.saasai.service;

import java.io.IOException;


import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.saasai.feature.ocr.GoogleVisionClient;


@Service
public class VisionOcrService {

    private final GoogleVisionClient googleVisionClient;

    public VisionOcrService(GoogleVisionClient googleVisionClient) {
        this.googleVisionClient = googleVisionClient;
    }


    public String extractText(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ảnh OCR không được để trống"
            );
        }

        byte[] imageBytes = image.getBytes();

        // Gọi Google Cloud Vision DOCUMENT_TEXT_DETECTION tại đây.
        // Không gọi CreditService.
        return googleVisionClient.detectDocumentText(imageBytes);
    }

}