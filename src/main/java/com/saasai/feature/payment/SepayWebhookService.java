package com.saasai.feature.payment;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SepayWebhookService {

    private static final Pattern ORDER_CODE_PATTERN =
            Pattern.compile("\\b(SAASAI[0-9A-Z]+)\\b");

    private final BillingService billingService;

    @Value("${sepay.webhook.secret}")
    private String webhookSecret;

    public void processWebhook(
            String authorization,
            SepayWebhookRequestDTO request) {

        // 1. Validate API key
        String expectedAuthorization =
                "Apikey " + webhookSecret;

        if (authorization == null
                || !authorization.trim().equals(expectedAuthorization)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized: invalid sepay webhook key"
            );
        }

        // 2. Validate payload
        if (request == null) {
            throw new IllegalArgumentException(
                    "Invalid webhook request"
            );
        }

        // 3. Chỉ xử lý tiền vào
        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            return;
        }

        // 4. Build searchable content
        // Theo demo: content + referenceCode + description
        String combined = Stream.of(
                        request.getContent(),
                        request.getCode(),
                        request.getDescription()
                )
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));

        // 5. Normalize
        String normalized = combined
                .replaceAll("[^A-Za-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase();

        // 6. Extract order code
        String orderCode = extractOrderCode(normalized);

        if (orderCode == null || orderCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing or unrecognized order code in webhook payload"
            );
        }

        // 7. Transaction ID
        String transactionId = getTransactionId(request);

        if (transactionId == null) {
            throw new IllegalArgumentException(
                    "Missing transaction ID"
            );
        }

        // 8. Map to internal billing DTO
        BillingWebhookRequestDTO billingRequest =
                new BillingWebhookRequestDTO();

        billingRequest.setContent(orderCode);
        billingRequest.setAmount(request.getTransferAmount());
        billingRequest.setTransactionId(transactionId);
        billingRequest.setTransactionDate(request.getTransactionDate());

        // 9. Gọi Billing business
        billingService.handleInvoiceWebhook(
                billingRequest
        );
    }

    private String extractOrderCode(String text) {

        if (text == null) {
            return null;
        }

        Matcher matcher = ORDER_CODE_PATTERN.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String getTransactionId(
            SepayWebhookRequestDTO request) {

        if (request.getId() != null) {
            return String.valueOf(request.getId());
        }

        if (request.getReferenceCode() != null
                && !request.getReferenceCode().isBlank()) {

            return request.getReferenceCode();
        }

        return null;
    }
}