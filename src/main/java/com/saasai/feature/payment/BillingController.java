package com.saasai.feature.payment;

import com.saasai.feature.ai.ApiResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.saasai.repository.BillingInvoiceRepository;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.repository.AdminPackageConfigRepository;
import com.saasai.entity.BillingInvoice;
import com.saasai.entity.User;
import com.saasai.service.UserService;
import com.saasai.feature.payment.BillingService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import java.util.Map;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing")
@CrossOrigin
public class BillingController {
    @Autowired
    private BillingService billingService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminPackageConfigRepository adminPackageConfigRepository;

    @Autowired
    private BillingInvoiceRepository billingInvoiceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/invoices")
    public ResponseEntity<ApiResponseDTO<BillingInvoiceDTO>> createInvoice(
            @RequestBody BillingInvoiceRequestDTO request
    ) {

        Integer duration = request.getDurationMonths();

        if (duration == null) {
        duration = 1;
        }

        if (duration != 1 && duration != 6 && duration != 12) {
        throw new IllegalArgumentException(
                "Thời hạn subscription chỉ được 1, 6 hoặc 12 tháng"
        );
        }

        String userId = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userService.getUserById(userId);

        AdminPackageConfig pkg = adminPackageConfigRepository.findByPackageType(request.getPackageType())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gói không tồn tại: " + request.getPackageType()));


        BillingInvoice invoice;
        if (pkg.getPackageCategory() == AdminPackageConfig.PackageCategory.CREDIT_PACK) {
                invoice = billingService.createCreditPackInvoice(
                        user,
                        pkg.getPackageType()
                );
        } else {
                invoice = billingService.createInvoice(user, pkg.getPackageType(), duration);
        }

        BillingInvoiceDTO responseData = convertToDTO(invoice);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Tạo hoá đơn thành công",
                        responseData
                )
        );
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponseDTO<Object>> billingWebhook(@RequestBody BillingWebhookRequestDTO request) {
        billingService.handleInvoiceWebhook(request);
        
        return ResponseEntity.ok(ApiResponseDTO.builder()
                .success(true)
                .message("Invoice webhook processed")
                .statusCode(200)
                .build());
    }

    // Chuyển đổi đối tượng BillingInvoice sang BillingInvoiceDTO
    private BillingInvoiceDTO convertToDTO(BillingInvoice invoice) {
        if (invoice == null) {
                return null;
        }

        PaymentInfoDTO paymentInfo = null;

        if (invoice.getQrBankSnapshot() != null
                && !invoice.getQrBankSnapshot().isBlank()) {
                try {
                Map<String, Object> snapshot =
                        objectMapper.readValue(
                                invoice.getQrBankSnapshot(),
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                        );

                paymentInfo = PaymentInfoDTO.builder()
                        .bankCode((String) snapshot.get("bankCode"))
                        .accountNumber((String) snapshot.get("accountNumber"))
                        .accountName((String) snapshot.get("accountName"))
                        .amount(invoice.getFinalAmount())
                        .additionalInfo(invoice.getMemoId())
                        .build();

                } catch (Exception exception) {
                paymentInfo = null;
                }
        }

        return BillingInvoiceDTO.builder()
                .invoiceId(invoice.getInvoiceId())
                .userId(invoice.getUser() != null
                        ? invoice.getUser().getUserId()
                        : null)
                .packageType(invoice.getAdminPackageConfig() != null
                        ? invoice.getAdminPackageConfig().getPackageType()
                        : "FREE")
                .durationMonths(invoice.getDurationMonths())
                .originalAmount(invoice.getOriginalAmount())
                .discountAmount(invoice.getDiscountAmount())
                .finalAmount(invoice.getFinalAmount())
                .memoId(invoice.getMemoId())
                .qrCodeUrl(invoice.getQrCodeUrl())
                .paymentInfo(paymentInfo)
                .status(invoice.getStatus() != null
                        ? invoice.getStatus().toString()
                        : null)
                .createdAt(invoice.getCreatedAt())
                .paymentDate(invoice.getPaymentDate())
                .build();
        }
        
    @GetMapping("/invoices/{invoiceId}/status")
    public ResponseEntity<ApiResponseDTO<Map<String,Object>>> getInvoiceStatus(@PathVariable String invoiceId) {
        BillingInvoice invoice = billingInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("invoiceId", invoice.getInvoiceId());
        payload.put(
                "status",
                invoice.getStatus() != null
                        ? invoice.getStatus().name()
                        : null
        );
        payload.put("finalAmount", invoice.getFinalAmount());
        payload.put("paymentDate", invoice.getPaymentDate());
        return ResponseEntity.ok(ApiResponseDTO.success("OK", payload));
        }

}
