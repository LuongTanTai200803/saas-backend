package com.saasai.ai;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class LocalAiProvider implements AiProvider {
    @Override
    public List<String> streamCompletion(String prompt, String model) throws IOException {
        String firstChunk = "### 1. VĂN BẢN HOÀN CHỈNH\n" +
                "Nội dung được xử lý bởi AI...";
        String secondChunk = "### 4. GHI CHÚ KIỂM TRA TRƯỚC KHI TRÌNH KÝ\n" +
                "Đã hoàn tất kiểm tra...";
        return List.of(firstChunk, secondChunk);
    }
}
