package com.saasai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(OpenRouterProperties.class)
public class OpenRouterConfig {

private static final Logger log =
        LoggerFactory.getLogger(OpenRouterConfig.class);

    @Bean
    public WebClient openRouterWebClient(
            WebClient.Builder builder,
            OpenRouterProperties properties
    ) {
        validate(properties);
        
        log.warn("===== OPENROUTER WEBCLIENT BEAN IS BEING CREATED =====");

        String apiKey = properties.apiKey();

        // log.warn(
        //     "OpenRouter key diagnostics: present={}, length={}, startsWithSkOr={}",
        //     StringUtils.hasText(properties.apiKey()),
        //     properties.apiKey() == null ? 0 : properties.apiKey().length(),
        //     properties.apiKey() != null && properties.apiKey().startsWith("sk-or-")
        // );

        log.warn(
                "OpenRouter timeout: {} ms, duration={}",
                properties.timeout().toMillis(),
                properties.timeout()
        );

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.timeout());

        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.apiKey()
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        "HTTP-Referer",
                        properties.siteUrl()
                )
                .defaultHeader(
                        "X-OpenRouter-Title",
                        properties.appName()
                )
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient)
                )
                .build();
    }

    private void validate(OpenRouterProperties properties) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException(
                    "OPENROUTER_API_KEY chưa được cấu hình"
            );
        }

        if (properties.timeout() == null
                || properties.timeout().isZero()
                || properties.timeout().isNegative()) {
            throw new IllegalStateException(
                    "OPENROUTER_TIMEOUT phải lớn hơn 0"
            );
        }
    }
}