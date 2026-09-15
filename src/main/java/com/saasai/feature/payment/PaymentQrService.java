package com.saasai.feature.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class PaymentQrService {

    private final PaymentBankConfigRepository bankConfigRepository;
    private final ObjectMapper objectMapper;

    public PaymentQrService(
            PaymentBankConfigRepository bankConfigRepository,
            ObjectMapper objectMapper
    ) {
        this.bankConfigRepository = bankConfigRepository;
        this.objectMapper = objectMapper;
    }

    public QrPayload generate(Long amount, String memoId) {
        PaymentBankConfig config =
                bankConfigRepository.findFirstByIsActiveTrue()
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        INTERNAL_SERVER_ERROR,
                                        "Chưa cấu hình tài khoản thanh toán"
                                ));

        String qrUrl = buildVietQrUrl(config, amount, memoId);
        String bankSnapshot = buildBankSnapshotJson(config);

        return new QrPayload(qrUrl, bankSnapshot);
    }

    private String buildVietQrUrl(
            PaymentBankConfig config,
            Long amount,
            String memoId
    ) {
        String bank = valueOrEmpty(config.getBankCode());
        String accountNumber = valueOrEmpty(config.getAccountNumber());
        String accountName = valueOrEmpty(config.getAccountName());

        String amountValue = amount == null
                ? "0"
                : String.valueOf(amount);

        String additionalInfo = memoId == null
                ? ""
                : memoId;

        return "https://img.vietqr.io/image/"
                + bank
                + "-"
                + accountNumber
                + "-qr_only.png?"
                + "amount="
                + encode(amountValue)
                + "&addInfo="
                + encode(additionalInfo)
                + "&accountName="
                + encode(accountName);
    }

    private String buildBankSnapshotJson(
            PaymentBankConfig config
    ) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("bankCode", config.getBankCode());
            snapshot.put("accountNumber", config.getAccountNumber());
            snapshot.put("accountName", config.getAccountName());
            snapshot.put("vaNumber", config.getVaNumber());
            snapshot.put("template", config.getTemplate());
            snapshot.put("store", config.getStore());

            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value == null ? "" : value,
                StandardCharsets.UTF_8
        );
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    public record QrPayload(
            String qrCodeUrl,
            String bankSnapshot
    ) {
    }
}