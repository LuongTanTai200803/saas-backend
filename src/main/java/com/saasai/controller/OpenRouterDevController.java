package com.saasai.controller;

import com.saasai.feature.ai.AiProviderResultDTO;
import com.saasai.service.OpenRouterSmokeTestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/openrouter")
public class OpenRouterDevController {

    private final OpenRouterSmokeTestService service;

    public OpenRouterDevController(
            OpenRouterSmokeTestService service
    ) {
        this.service = service;
    }

    @GetMapping("/test")
    public AiProviderResultDTO test(
            @RequestParam String model
    ) {
        return service.test(model);
    }
}