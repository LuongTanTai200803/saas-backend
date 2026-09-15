package com.saasai.feature.ai;

import com.saasai.feature.ai.TemplateRouteResult;
import com.saasai.feature.ai.TemplateRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TemplateRoutingService {

    private static final String COMMON_TEMPLATE = "COMMON";

    private final TemplateRuleLoader templateRuleLoader;

    public TemplateRouteResult resolve(
            String topicCode,
            String documentTitle
    ) {
        String normalizedTitle =
                normalize(documentTitle);

        System.out.println("\n========== TEMPLATE ROUTING ==========");
        System.out.println("Topic          : " + topicCode);
        System.out.println("Original title : " + documentTitle);
        System.out.println("Normalized     : " + normalizedTitle);

        List<TemplateRule> rules =
                templateRuleLoader.loadRules(topicCode);

        // System.out.println("\n========== TEMPLATE RULES ==========");

        // for (TemplateRule rule : rules) {
        // System.out.println(
        //         "Template : " + rule.templateCode()
        //         + " | Priority : " + rule.priority()
        //         + " | Keywords : " + rule.titleKeywords()
        // );
        // }

        for (TemplateRule rule : rules) {
            if (matches(normalizedTitle, rule)) {

                System.out.println(
                        ">>> MATCH TEMPLATE: "
                                + rule.templateCode()
                );
                System.out.println("=======================================\n");

                return new TemplateRouteResult(
                        topicCode,
                        rule.templateCode(),
                        true
                );
            }
        }

        return new TemplateRouteResult(
                topicCode,
                COMMON_TEMPLATE,
                false
        );
    }

//     private boolean matches(
//             String normalizedTitle,
//             TemplateRule rule
//     ) {
//         if (rule.titleKeywords() == null
//                 || rule.titleKeywords().isEmpty()) {
//             return false;
//         }

//         return rule.titleKeywords().stream()
//                 .filter(keyword -> keyword != null
//                         && !keyword.isBlank())
//                 .map(this::normalize)
//                 .anyMatch(normalizedTitle::contains);
//     }

        private boolean matches(
                String normalizedTitle,
                TemplateRule rule
        ) {
        if (rule.titleKeywords() == null
                || rule.titleKeywords().isEmpty()) {
                return false;
        }

        return rule.titleKeywords().stream()
                .filter(keyword ->
                        keyword != null && !keyword.isBlank()
                )
                .map(this::normalize)
                .anyMatch(keyword -> {

                        boolean matched = Pattern.compile(
                                "(^|\\s)" +
                                Pattern.quote(keyword) +
                                "(\\s|$)"
                        ).matcher(normalizedTitle).find();

                        // System.out.println(
                        //         "CHECK TEMPLATE: " +
                        //         rule.templateCode() +
                        //         " | keyword=[" + keyword + "]" +
                        //         " | matched=" + matched
                        // );

                        return matched;
                });
        }
    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String lowerCase = value
                .toLowerCase(Locale.ROOT)
                .trim();

        String normalized = Normalizer.normalize(
                lowerCase,
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replaceAll("\\s+", " ")
                .trim();
    }
}