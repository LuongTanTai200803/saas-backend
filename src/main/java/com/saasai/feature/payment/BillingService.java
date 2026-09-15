package com.saasai.feature.payment;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.saasai.admin.AdminService;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.AdminPackageConfig.PackageCategory;
import com.saasai.entity.BillingInvoice;
import com.saasai.entity.BillingInvoice.InvoiceType;
import com.saasai.entity.CreditTransaction;
import com.saasai.entity.CreditTransaction.TransactionType;
import com.saasai.entity.TransactionRecord;
import com.saasai.entity.User;
import com.saasai.repository.BillingInvoiceRepository;
import com.saasai.repository.CreditTransactionRepository;
import com.saasai.repository.TransactionRecordRepository;
import com.saasai.repository.UserRepository;
import com.saasai.repository.AdminPackageConfigRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.web.server.ResponseStatusException;


@Service
public class BillingService {
    @Autowired
    private BillingInvoiceRepository billingInvoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminPackageConfigRepository adminPackageConfigRepository; // Đã thêm để truy vấn gói động

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private MonthlyQuotaPolicy monthlyQuotaPolicy;

    @Autowired
    private PaymentBankConfigRepository paymentBankConfigRepository;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper; // or new ObjectMapper()
    private final PaymentQrService paymentQrService;

    @Autowired
    public BillingService(PaymentQrService paymentQrService) {
        this.paymentQrService = paymentQrService;
    }

    private static final Logger logger = LoggerFactory.getLogger(BillingService.class);

    @Transactional
    public BillingInvoice createInvoice(User user, String rawPackageType, Integer months) {
        String normalizedType = normalizePackageType(rawPackageType);

        AdminPackageConfig packageConfig = adminPackageConfigRepository
                .findByPackageType(normalizedType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gói không tồn tại: " + normalizedType));

        validateSubscriptionUpgrade(user, packageConfig);

        String memoId = "SAASAI" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);

        Long originalAmount = calculateOriginalAmount(packageConfig, months);
        Long discountAmount = calculateDiscount(originalAmount, months);
        Long finalAmount = originalAmount - discountAmount;

        BillingInvoice.BillingInvoiceBuilder builder = BillingInvoice.builder()
                .user(user)
                .adminPackageConfig(packageConfig)
                .durationMonths(months)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .memoId(memoId)
                .invoiceType(InvoiceType.SUBSCRIPTION);

        builder = attachQrToBuilder(builder, finalAmount, memoId);

        BillingInvoice invoice = builder.build();
        return billingInvoiceRepository.save(invoice);
        }

    @Transactional
    public BillingInvoice createCreditPackInvoice(User user, String packageType) {
        AdminPackageConfig packageConfig = adminPackageConfigRepository
                .findByPackageType(packageType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gói không tồn tại: " + packageType));

        if (packageConfig.getPackageCategory() != PackageCategory.CREDIT_PACK) {
                throw new IllegalArgumentException("Package không phải Credit Pack");
        }

        String memoId = "SAASAI" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);

        Long price = packageConfig.getPrice();

        BillingInvoice.BillingInvoiceBuilder builder = BillingInvoice.builder()
                .user(user)
                .adminPackageConfig(packageConfig)
                .invoiceType(InvoiceType.CREDIT_PACK)
                .durationMonths(1)
                .originalAmount(price)
                .discountAmount(0L)
                .finalAmount(price)
                .memoId(memoId);

        builder = attachQrToBuilder(builder, price, memoId);

        BillingInvoice invoice = builder.build();
        return billingInvoiceRepository.save(invoice);
        }

    @Transactional
    public void processPaymentWebhook(BillingWebhookRequestDTO webhookData) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        if (webhookData == null || webhookData.getContent() == null) {
            return;
        }

        String memo = webhookData.getContent().toString().trim();

                BillingInvoice invoice = billingInvoiceRepository.findByMemoId(memo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn..."));

        // =========================================================
        // 1. Check invoice status
        // =========================================================
        if (invoice.getStatus() == BillingInvoice.InvoiceStatus.CANCELLED
                || invoice.getStatus() == BillingInvoice.InvoiceStatus.EXPIRED) {

            logger.info(
                    "Invoice {} not payable (status={}), ignoring webhook",
                    invoice.getInvoiceId(),
                    invoice.getStatus()
            );

            return;
        }
        String txId = webhookData.getTransactionId();
        // =========================================================
        // 2. Invoice đã PAID -> duplicate webhook
        // =========================================================
        if (invoice.getStatus() == BillingInvoice.InvoiceStatus.PAID) {

            logger.info(
                    "Invoice {} already PAID, ignoring webhook tx={}",
                    invoice.getInvoiceId(),
                    txId
            );

            return;
        }
        // =========================================================
        // 3. Idempotency theo external transaction ID
        // =========================================================
        if (txId != null && !txId.isBlank()) {

            Optional<TransactionRecord> existing =
                    transactionRecordRepository.findByExternalTransactionId(txId);

            if (existing.isPresent()) {

                TransactionRecord existingTransaction = existing.get();

                if (!existingTransaction.getInvoiceId().equals(invoice.getInvoiceId())) {

                    logger.warn(
                            "Transaction {} already applied to another invoice {} (current {}). Ignoring.",
                            txId,
                            existingTransaction.getInvoiceId(),
                            invoice.getInvoiceId()
                    );

                    throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Transaction " + txId + " already applied to another invoice " + existingTransaction.getInvoiceId()
                );

                } else {

                    logger.info(
                            "Transaction {} already applied to invoice {} - ignoring duplicate webhook.",
                            txId,
                            invoice.getInvoiceId()
                    );
                }

                return;
            }
        }
        // =========================================================
        // 4. Validate amount
        // =========================================================
        BigDecimal invoiceAmount = invoice.getFinalAmount() != null
                ? BigDecimal.valueOf(invoice.getFinalAmount())
                : BigDecimal.ZERO;

        BigDecimal webhookAmount = webhookData.getAmount();

        if (webhookAmount == null
                || invoiceAmount.compareTo(webhookAmount) != 0) {

            logger.warn(
                    "Amount mismatch for invoice {}: expected={}, got={}, tx={}",
                    invoice.getInvoiceId(),
                    invoiceAmount,
                    webhookAmount,
                    txId
            );

            throw new IllegalArgumentException(
                    "Số tiền thanh toán không khớp với hóa đơn"
            );
        }
        // =========================================================
        // 5. Mark invoice PAID
        // =========================================================

        invoice.setStatus(BillingInvoice.InvoiceStatus.PAID);
        invoice.setPaymentDate(now);

        billingInvoiceRepository.save(invoice);
        // =========================================================
        // 6. Save payment transaction
        //    transactions = PAYMENT HISTORY
        // =========================================================
        TransactionRecord record = TransactionRecord.builder()
                .user(invoice.getUser())
                .invoiceId(invoice.getInvoiceId())
                .memoId(invoice.getMemoId())
                .amount(webhookAmount)
                .status("PAID")
                .externalTransactionId(txId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
                transactionRecordRepository.save(record);
        } catch (DataIntegrityViolationException ex) {
                logger.warn("Transaction save failed (possible duplicate) tx={} invoice={} : {}", txId, invoice.getInvoiceId(), ex.getMessage());
                // Nếu duplicate đã được áp dụng, return/ignore an toàn
                return;
        }
        // =========================================================
        // 7. Load User
        // =========================================================
        User user = userRepository.findById(invoice.getUser().getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "Người dùng không tồn tại"
                ));
        // =========================================================
        // 8. Process product
        // =========================================================
        if (invoice.getInvoiceType() == InvoiceType.SUBSCRIPTION) {

            processSubscriptionPayment(invoice, user, now);

        } else if (invoice.getInvoiceType() == InvoiceType.CREDIT_PACK) {

            processCreditPackPayment(invoice, user, now);

        } else {

            throw new IllegalArgumentException(
                    "Loại hóa đơn không được hỗ trợ: "
                            + invoice.getInvoiceType()
            );
        }
        // =========================================================
        // 9. Save User
        // =========================================================
        userRepository.save(user);
    }

    private String normalizePackageType(String packageType) {
        if (packageType == null || packageType.trim().isEmpty()) {
            return "FREE";
        }

        String trimmedUpper = packageType.trim().toUpperCase();
        return switch (trimmedUpper) {
            case "TRIAL", "FREE" -> "FREE";
            case "BASIC" -> "BASIC";
            case "PROFESSIONAL" -> "PROFESSIONAL";
            case "ENTERPRISE" -> "ENTERPRISE";
            default -> trimmedUpper;
        };
    }

    // xử lý thanh toán gói subscription
    private void processSubscriptionPayment(
            BillingInvoice invoice,
            User user,
            LocalDateTime now) {

        AdminPackageConfig targetPackage = invoice.getAdminPackageConfig();

        if (targetPackage == null) {
            throw new IllegalArgumentException(
                    "Hóa đơn không có package subscription"
            );
        }

        // Lấy số tháng từ hóa đơn, nếu null thì mặc định 1 tháng
        int durationMonths = invoice.getDurationMonths() != null
                ? invoice.getDurationMonths()
                : 1;
        // ---------------------------------------------------------
        // 1. Reset monthly quota theo package mới
        // ---------------------------------------------------------
        monthlyQuotaPolicy.allocateSubscriptionCredits(
                user,
                targetPackage
        );
        // ---------------------------------------------------------
        // 2. Đổi package ngay lập tức
        // ---------------------------------------------------------
        user.setAdminPackageConfig(targetPackage);

        // ---------------------------------------------------------
        // 3. Extend subscription expiration
        //
        // Nếu gói hiện tại còn hạn:
        //     currentExpire + duration
        //
        // Nếu đã hết hạn:
        //     now + duration
        // ---------------------------------------------------------
        LocalDateTime currentExpire = user.getExpireDate();

        LocalDateTime newExpire;

        if (currentExpire != null && currentExpire.isAfter(now)) {
            newExpire = currentExpire.plusMonths(durationMonths);
        } else {
            newExpire = now.plusMonths(durationMonths);
        }
        user.setExpireDate(newExpire);

        logger.info(
                "Subscription activated: user={}, package={}, quota={}, expire={}",
                user.getUserId(),
                targetPackage.getPackageType(),
                targetPackage.getCreditLimit(),
                newExpire
        );
    }

    // xử lý thanh toán gói credit pack
    private void processCreditPackPayment(
            BillingInvoice invoice,
            User user,
            LocalDateTime now) {

        AdminPackageConfig packageConfig = invoice.getAdminPackageConfig();

        if (packageConfig == null) {
            throw new IllegalArgumentException(
                    "Hóa đơn không có package credit pack"
            );
        }

        if (packageConfig.getCreditLimit() == null
                || packageConfig.getCreditLimit() <= 0) {
            throw new IllegalArgumentException(
                    "Credit pack không có số credit hợp lệ"
            );
        }

        if (packageConfig.getDuration() == null
                || packageConfig.getDuration() <= 0) {
            throw new IllegalArgumentException(
                    "Credit pack không có thời hạn hợp lệ"
            );
        }

        double credits = packageConfig.getCreditLimit();
        int durationDays = packageConfig.getDuration();

        // Cấp credit pack cho user khi thanh toán gói credit pack thành công
        monthlyQuotaPolicy.allocateCreditPack(
                user,
                credits,
                durationDays
        );

        logger.info(
                "Credit pack activated: user={}, credits={}, durationDays={}",
                user.getUserId(),
                credits,
                durationDays
        );
    }

    private void validateSubscriptionUpgrade(
            User user,
            AdminPackageConfig targetPackage) {

        AdminPackageConfig currentPackage =
                user.getAdminPackageConfig();

        LocalDateTime now = LocalDateTime.now();

        // Không có subscription hiện tại hoặc đã hết hạn
        if (currentPackage == null
                || user.getExpireDate() == null
                || !now.isBefore(user.getExpireDate())) {
            return;
        }

        // Nếu subscription hiện tại vẫn còn hạn
        // thì chỉ được mua package cao hơn
        int currentRank = getPackageRank(
                currentPackage.getPackageType()
        );

        int targetRank = getPackageRank(
                targetPackage.getPackageType()
        );

        if (targetRank <= currentRank) {
            throw new IllegalArgumentException(
                    "Chỉ được nâng cấp lên gói cao hơn gói hiện tại"
            );
        }
    }
    private int getPackageRank(String packageType) {
        return switch (packageType.toUpperCase()) {
            case "FREE" -> 0;
            case "STANDARD" -> 1;
            case "PREMIUM" -> 2;
            default -> throw new IllegalArgumentException(
                    "Package type không hợp lệ: " + packageType
            );
        };
    }


    // Đã sửa đổi: Không dùng switch-case giá cứng, lấy trực tiếp giá từ thực thể DB
    private Long calculateOriginalAmount(AdminPackageConfig packageConfig, Integer months) {
        if (packageConfig == null || months == null) {
            return 0L;
        }
        return packageConfig.getPrice() * months;
    }

    private Long calculateDiscount(Long originalAmount, Integer months) {
        if (months != null && months >= 12) {
            return (long) (originalAmount * 0.2); // Giảm giá 20% khi mua gói theo năm
        }
        return 0L;
    }

    @Transactional
    public void handleInvoiceWebhook(BillingWebhookRequestDTO request) {
        // validate
        // tìm invoice
        // kiểm tra duplicate
        // kiểm tra amount
        // xử lý thanh toán 
        if (request == null
            || !StringUtils.hasText(request.getContent())
            || request.getAmount() == null) {

        throw new IllegalArgumentException("Dữ liệu webhook không hợp lệ");
    }
        processPaymentWebhook(request);
    }

    public void processPaidInvoice(String testInvoiceId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processPaidInvoice'");
    }


    private BillingInvoice.BillingInvoiceBuilder attachQrToBuilder(
        BillingInvoice.BillingInvoiceBuilder builder,
        Long amount,
        String memoId
        ) {
        PaymentQrService.QrPayload qr =
                paymentQrService.generate(amount, memoId);

        return builder
                .qrCodeUrl(qr.qrCodeUrl())
                .qrBankSnapshot(qr.bankSnapshot());
        }

}
