package com.saasai.feature.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.feature.ai.TemplateRule;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateRuleLoader {

    private final ObjectMapper objectMapper;
    private final AiResourceService aiResourceService;

    public List<TemplateRule> loadRules(String topicCode) {
        String rulesPath =
                aiResourceService.getRulesPath(topicCode);

        ClassPathResource resource =
                new ClassPathResource(rulesPath);

        if (!resource.exists()) {
            return List.of();
        }

        try {
            List<TemplateRule> rules =
                    objectMapper.readValue(
                            resource.getInputStream(),
                            new TypeReference<List<TemplateRule>>() {
                            }
                    );

            if (rules == null) {
                return List.of();
            }

            return rules.stream()
                    .sorted(
                            Comparator.comparing(
                                    TemplateRule::priority,
                                    Comparator.nullsLast(
                                            Comparator.reverseOrder()
                                    )
                            )
                    )
                    .toList();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Không thể đọc rules.json của topic "
                            + topicCode,
                    exception
            );
        }
    }
}