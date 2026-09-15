package com.saasai.feature.ai;

import com.saasai.feature.ai.AiTemplateContent;
import com.saasai.feature.ai.TemplateRouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTemplateService {

    private final TemplateRoutingService templateRoutingService;
    private final AiResourceService aiResourceService;

    /**
     * Resolve the template content for a given topic code and document title.
     *
     * @param topicCode     the code representing the topic of the document.
     * @param documentTitle the title of the document for which the template is to be resolved.
     * @return An AiTemplateContent object containing the resolved template content and related information.
     */
    public AiTemplateContent resolve(
            String topicCode,
            String documentTitle
    ) {
        // Load file mẫu txt theo topicCode và templateCode
        TemplateRouteResult route =
                templateRoutingService.resolve(
                        topicCode,
                        documentTitle
                );

        String globalInstruction =
                aiResourceService
                        .loadGlobalSystemInstruction();
        // Load topic prompt and template content based on the resolved route
        String topicPrompt =
                aiResourceService
                        .loadTopicPrompt(route.topicCode());

        String templateContent =
                aiResourceService.loadTemplate(
                        route.topicCode(),
                        route.templateCode()
                );

        return new AiTemplateContent(
                route.topicCode(),
                route.templateCode(),
                globalInstruction,
                topicPrompt,
                templateContent
        );
    }
}