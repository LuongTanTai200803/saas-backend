package com.saasai.controller;

import com.saasai.feature.ai.TemplateRouteResult;
import com.saasai.feature.ai.TemplateRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dev/template-routing")
@RequiredArgsConstructor
@Profile("dev")
public class TemplateRoutingDevController {

    private final TemplateRoutingService templateRoutingService;

    @GetMapping
    public TemplateRouteResult resolve(
            @RequestParam String topicCode,
            @RequestParam String title
    ) {
        return templateRoutingService.resolve(
                topicCode,
                title
        );
    }
}