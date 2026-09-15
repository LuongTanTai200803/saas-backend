package com.saasai.feature.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.config.OpenRouterProperties;
import com.saasai.feature.ai.AiProviderResultDTO;
import com.saasai.feature.ai.openrouter.OpenRouterChoiceDTO;
import com.saasai.feature.ai.openrouter.OpenRouterErrorResponseDTO;
import com.saasai.feature.ai.openrouter.OpenRouterRequestDTO;
import com.saasai.feature.ai.openrouter.OpenRouterResponseDTO;
import com.saasai.feature.ai.openrouter.OpenRouterUsageDTO;
import com.saasai.exception.OpenRouterEmptyResponseException;
import com.saasai.exception.OpenRouterException;
import com.saasai.exception.OpenRouterTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;


@Component
public class OpenRouterClient {

    private static final String CHAT_COMPLETIONS_PATH =
            "/chat/completions";

    private final OpenRouterProperties openRouterProperties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public OpenRouterClient(
            OpenRouterProperties openRouterProperties,
            ObjectMapper objectMapper,
            @Qualifier("openRouterWebClient") WebClient webClient
    ) {
        this.openRouterProperties = openRouterProperties;
        this.objectMapper = objectMapper;
        this.webClient = webClient;
    }

    public AiProviderResultDTO complete(OpenRouterRequestDTO request) {
        try {
            OpenRouterResponseDTO response = webClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            clientResponse -> clientResponse
                                    .bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> createHttpException(
                                            clientResponse.statusCode().value(),
                                            body
                                    ))
                    )
                    .bodyToMono(OpenRouterResponseDTO.class)
                    .block();

                System.out.println("===== OPENROUTER RAW RESPONSE =====");
                System.out.println(response);

            return validateAndMap(response);

        } catch (OpenRouterException exception) {
            throw exception;

        } catch (Exception exception) {
            Throwable rootCause = Exceptions.unwrap(exception);

            if (isTimeout(rootCause)) {
                throw new OpenRouterTimeoutException(
                        "OpenRouter không phản hồi trong thời gian cho phép",
                        rootCause
                );
            }

            throw new OpenRouterException(
                    502,
                    "OPENROUTER_CLIENT_ERROR",
                    "Không thể kết nối OpenRouter: "
                            + rootCause.getMessage(),
                    true
            );
        }
    }

    private AiProviderResultDTO validateAndMap(
            OpenRouterResponseDTO response
    ) {
        if (response == null) {
            throw new OpenRouterEmptyResponseException(
                    "OpenRouter trả response null"
            );
        }

        if (response.choices() == null
                || response.choices().isEmpty()) {
            throw new OpenRouterEmptyResponseException(
                    "OpenRouter không trả choices"
            );
        }

        OpenRouterChoiceDTO choice = response.choices().get(0);

        if ("error".equalsIgnoreCase(choice.finishReason())) {
            String message = choice.error() != null
                    ? choice.error().message()
                    : "OpenRouter kết thúc với finish_reason=error";

            throw new OpenRouterException(
                    choice.error() != null
                            && choice.error().code() != null
                            ? choice.error().code()
                            : 502,
                    "OPENROUTER_GENERATION_ERROR",
                    message,
                    true
            );
        }

        if (choice.message() == null
                || choice.message().content() == null
                || choice.message().content().isBlank()) {
            throw new OpenRouterEmptyResponseException(
                    "OpenRouter trả nội dung rỗng"
            );
        }

        OpenRouterUsageDTO usage = response.usage();

        return new AiProviderResultDTO(
                choice.message().content(),
                response.model(),
                choice.finishReason(),
                usage != null ? usage.promptTokens() : 0,
                usage != null ? usage.completionTokens() : 0,
                usage != null ? usage.totalTokens() : 0,
                null
        );
    }

    private OpenRouterException createHttpException(
            int statusCode,
            String responseBody
    ) {
        String message = extractErrorMessage(responseBody);
        boolean fallbackAllowed = shouldFallback(statusCode);

        return new OpenRouterException(
                statusCode,
                mapErrorCode(statusCode),
                message,
                fallbackAllowed
        );
    }

    private boolean shouldFallback(int statusCode) {
        return statusCode == 408
                || statusCode == 429
                || statusCode == 500
                || statusCode == 502
                || statusCode == 503
                || statusCode == 504;
    }

    private String mapErrorCode(int statusCode) {
        return switch (statusCode) {
            case 400 -> "OPENROUTER_BAD_REQUEST";
            case 401 -> "OPENROUTER_UNAUTHORIZED";
            case 402 -> "OPENROUTER_INSUFFICIENT_CREDITS";
            case 403 -> "OPENROUTER_FORBIDDEN";
            case 408 -> "OPENROUTER_TIMEOUT";
            case 429 -> "OPENROUTER_RATE_LIMITED";
            case 500, 502, 503, 504 ->
                    "OPENROUTER_PROVIDER_UNAVAILABLE";
            default -> "OPENROUTER_HTTP_ERROR";
        };
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "OpenRouter trả lỗi nhưng không có nội dung";
        }

        try {
            OpenRouterErrorResponseDTO response =
                    objectMapper.readValue(
                            responseBody,
                            OpenRouterErrorResponseDTO.class
                    );

            if (response.error() != null
                    && response.error().message() != null
                    && !response.error().message().isBlank()) {
                return response.error().message();
            }

        } catch (JsonProcessingException ignored) {
            // Không log API key hoặc request body.
        }

        return "OpenRouter request thất bại";
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof TimeoutException
                    || current instanceof ReadTimeoutException
                    || current instanceof ConnectException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

}