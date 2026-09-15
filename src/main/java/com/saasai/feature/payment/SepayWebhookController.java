package com.saasai.feature.payment;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sepay")
public class SepayWebhookController {

    private final SepayWebhookService sepayWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestBody SepayWebhookRequestDTO request) {

        sepayWebhookService.processWebhook(
                authorization,
                request
        );

        return ResponseEntity.ok("Webhook processed");
    }
}