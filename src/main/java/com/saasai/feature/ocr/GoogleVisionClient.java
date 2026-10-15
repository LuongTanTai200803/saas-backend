package com.saasai.feature.ocr;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import com.saasai.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
@Component
public class GoogleVisionClient {
        private static final Logger log = LoggerFactory.getLogger(GoogleVisionClient.class);
        

    public String detectDocumentText(byte[] imageBytes) {
        try (ImageAnnotatorClient client =
                     ImageAnnotatorClient.create()) {

            Image image = Image.newBuilder()
                    .setContent(ByteString.copyFrom(imageBytes))
                    .build();

            Feature feature = Feature.newBuilder()
                    .setType(
                            Feature.Type.DOCUMENT_TEXT_DETECTION
                    )
                    .build();

            AnnotateImageRequest request =
                    AnnotateImageRequest.newBuilder()
                            .setImage(image)
                            .addFeatures(feature)
                            .build();

            AnnotateImageResponse response =
                    client.batchAnnotateImages(
                            java.util.List.of(request)
                    )
                    .getResponses(0);

            if (response.hasError()) {
                throw new IllegalStateException(
                        response.getError().getMessage()
                );
            }

            return response.getFullTextAnnotation()
                    .getText();
        } catch (Exception exception) {
                log.error(
            "Google Vision không thể xử lý ảnh OCR",
            exception
    );
    
            throw new IllegalStateException(
                    "Google Vision OCR thất bại",
                    exception
            );
        }
    }
}