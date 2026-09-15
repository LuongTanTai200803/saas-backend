package com.saasai.service;

import com.saasai.feature.ai.client.OpenRouterClient;
import com.saasai.feature.ai.AiProviderResultDTO;
import com.saasai.feature.ai.openrouter.OpenRouterMessageDTO;
import com.saasai.feature.ai.openrouter.OpenRouterRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpenRouterSmokeTestService {


    private final OpenRouterClient openRouterClient;

    public OpenRouterSmokeTestService(
            OpenRouterClient openRouterClient
    ) {
        this.openRouterClient = openRouterClient;
    }

    public AiProviderResultDTO test(String model) {
        OpenRouterRequestDTO request =
                new OpenRouterRequestDTO(
                                model,
                                List.of(
                                new OpenRouterMessageDTO(
                                        "system",
                                        "Bạn là trợ lý. Trả lời ngắn gọn."
                                ),
                                new OpenRouterMessageDTO(
                                        "user",
                                        "Hãy trả lời đúng một từ: OK"
                                )
                ),
                        0.0,
                        20,
                        false
                );

        return openRouterClient.complete(request);
    }
}