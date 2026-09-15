package com.saasai.service;

import com.saasai.dto.CreditEstimateDTO;
import com.saasai.dto.CreditEstimateResponseDTO;
import com.saasai.dto.CreditSettleRequestDTO;
import com.saasai.entity.ChatSession;
import com.saasai.entity.CreditTransaction;
import com.saasai.entity.FileMetadata;
import com.saasai.entity.User;
import com.saasai.repository.ChatSessionRepository;
import com.saasai.repository.CreditTransactionRepository;
import com.saasai.repository.FileMetadataRepository;
import com.saasai.repository.UserRepository;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.saasai.exception.AuthException;
import com.saasai.feature.payment.CreditAccount;
import com.saasai.feature.payment.CreditAccountRepository;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CreditService {
    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetadataRepository fileUploadRepository;

    @Autowired
    private CreditAccountRepository creditAccountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Logger log = LoggerFactory.getLogger(CreditService.class);

    private final ChatSessionRepository chatSessionRepository;

    @Autowired
    public CreditService(ChatSessionRepository chatSessionRepository) {
        this.chatSessionRepository = chatSessionRepository;
    }


    private record AccountDeduction(
        double monthlyDeducted,
        double purchasedDeducted,
        double totalReserved
    ) {}

    private record HoldBreakdown(
            double monthlyDeducted,
            double purchasedDeducted
    ) {}

    public CreditEstimateResponseDTO estimateCredits(CreditEstimateDTO request) {
        User currentUser = userRepository.findByEmail(resolveCurrentEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        String modelName = request.getModelName() != null ? request.getModelName() : request.getModelSelected();
        List<String> features = request.getFeatures() != null ? request.getFeatures() : deriveLegacyFeatures(request);
        double legacyLengthCost = request.getInputLength() != null ? calculateLegacyLengthCost(request.getInputLength())
                : 0.0;

        double modelCost = calculateModelCost(modelName);
        double featureCost = calculateFeatureCost(features) + legacyLengthCost;
        double fileCost = calculateFileCost(request.getFileId(), currentUser);
        double estimatedCredits = roundOneDecimal(modelCost + featureCost + fileCost);

        CreditAccount account = creditAccountRepository
                .findById(currentUser.getUserId())
                .orElseThrow(() ->
                        new IllegalStateException("CreditAccount không tồn tại"));

        double monthly = account.getMonthlyQuotaRemaining() != null
                ? account.getMonthlyQuotaRemaining()
                : 0.0;

        double purchased = account.getPurchasedCreditBalance() != null
                ? account.getPurchasedCreditBalance()
                : 0.0;

        double currentCredits =
                roundOneDecimal(Math.max(0.0, monthly)
                        + Math.max(0.0, purchased));

        return CreditEstimateResponseDTO.builder()
                .estimatedCredits(estimatedCredits)
                .currentCredits(currentCredits)
                .isEligible(currentCredits >= estimatedCredits)
                .inputCreditEstimate(roundOneDecimal(modelCost + legacyLengthCost))
                .outputCreditEstimate(roundOneDecimal(featureCost - legacyLengthCost + fileCost))
                .totalCreditHold(estimatedCredits)
                .build();
    }

    private List<String> deriveLegacyFeatures(CreditEstimateDTO request) {
        if (request.getOutputOption() == null) {
            return Collections.emptyList();
        }
        return List.of(request.getOutputOption());
    }

    private double calculateModelCost(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return 3.0;
        }

        String normalized = modelName.toLowerCase(Locale.ROOT);
        if (normalized.contains("opus") || normalized.contains("gpt-5") || normalized.contains("o1")) {
            return 6.0;
        }
        if (normalized.contains("sonnet") || normalized.contains("gpt-4")) {
            return 4.0;
        }
        if (normalized.contains("haiku") || normalized.contains("mini")) {
            return 2.0;
        }
        return 3.0;
    }

    private double calculateFileCost(String rawFileId, User currentUser) {
        if (rawFileId == null || rawFileId.isBlank()) {
            throw new AuthException("file_id is required", HttpStatus.UNAUTHORIZED);
        }
        
        String fileId = parseFileId(rawFileId);
        FileMetadata fileUpload = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Tệp không tồn tại"));

        if (fileUpload.getUser() == null || !fileUpload.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("Bạn không có quyền sử dụng tệp này");
        }

        double fileSizeInMb = fileUpload.getFileSize() == null ? 0.0 : fileUpload.getFileSize() / (1024.0 * 1024.0);
        return roundOneDecimal(Math.max(0.5, Math.ceil(fileSizeInMb)));
    }

    private double calculateLegacyLengthCost(Double inputLength) {
        return roundOneDecimal((inputLength / 1000.0) * 0.15);
    }

    private double calculateFeatureCost(java.util.List<java.lang.String> features) {
        if (features == null || features.isEmpty()) {
            return 0.0;
        }
        return roundOneDecimal(features.size() * 0.75);
    }

    
    //  nếu bắt đầu bằng file_ thì cắt
    // UUID.fromString(normalized) để validate
    // return normalized (string UUID)

    private String parseFileId(String rawFileId) {
        String normalized = rawFileId.startsWith("file_") ? rawFileId.substring(5) : rawFileId;
        try {
            UUID.fromString(normalized); // validate UUID
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("fileId không hợp lệ");
        }
    }

    private String resolveCurrentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Không tìm thấy thông tin đăng nhập");
        }
        if (authentication.getDetails() instanceof String details && !details.isBlank()) {
            return details;
        }
        String name = authentication.getName();
        if (name != null && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
            return name;
        }
        throw new RuntimeException("Không tìm thấy email người dùng hiện tại");
    }

    // Tính giá trị làm tròn đến một chữ số thập phân
    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // create HOLD and save pricing snapshot

    @Transactional
    public CreditTransaction recordHoldTransactionWithSnapshot(
            String userId,
            Integer sessionId,
            CreditSettleRequestDTO requestDTO
    ) {
        ChatSession session = chatSessionRepository
                .findById(sessionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "ChatSession không tồn tại"
                        ));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        double holdAmount = requestDTO.getEstimatedHold() != null
                ? requestDTO.getEstimatedHold()
                : 0.0;

        if (holdAmount <= 0) {
            throw new IllegalArgumentException("Số credit HOLD phải lớn hơn 0");
        }

        AccountDeduction deduction =
                reserveFromCreditAccount(userId, holdAmount);

        String breakdown = String.format(
                "{\"monthlyDeducted\":%.1f,\"purchasedDeducted\":%.1f}",
                deduction.monthlyDeducted(),
                deduction.purchasedDeducted()
        );

        String description = requestDTO.getDescription() == null
                ? ""
                : requestDTO.getDescription();

        description += " HOLD_BREAKDOWN: " + breakdown;

        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .session(session)
                .model(requestDTO.getModel())
                .modelPackageId(requestDTO.getModelPackageId())
                .creditRate(requestDTO.getCreditRate())
                .outputWeight(requestDTO.getOutputWeight())
                .inputCredit(requestDTO.getInputCredit())
                .outputCredit(requestDTO.getOutputCredit())
                .totalCreditHold(deduction.totalReserved())
                .actualCreditDeducted(0.0)
                .refundedCredit(0.0)
                .description(description)
                .type(CreditTransaction.TransactionType.HOLD)
                .build();

        return creditTransactionRepository.save(transaction);
    }

    // Reserve credit from the user's account, prioritizing monthly quota first, then purchased credit.
    private AccountDeduction reserveFromCreditAccount(String userId, double amount) {
        CreditAccount acc = creditAccountRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("CreditAccount không tồn tại"));

        double monthly = acc.getMonthlyQuotaRemaining() != null
                ? acc.getMonthlyQuotaRemaining()
                : 0.0;

        double purchased = acc.getPurchasedCreditBalance() != null
                ? acc.getPurchasedCreditBalance()
                : 0.0;

        // Normalize số dư âm
        boolean corrected = false;

        if (monthly < 0) {
            log.warn(
                    "Monthly quota bị âm: userId={}, value={}. Reset về 0.",
                    userId,
                    monthly
            );

            amount = amount - monthly;
            monthly = 0.0;
            acc.setMonthlyQuotaRemaining(0.0);
            corrected = true;
        }

        if (purchased < 0) {
            log.warn(
                    "Purchased credit bị âm: userId={}, value={}. Reset về 0.",
                    userId,
                    purchased
            );
            purchased = 0.0;
            acc.setPurchasedCreditBalance(0.0);
            corrected = true;
        }

        if (corrected) {
            creditAccountRepository.save(acc);
        }

        // Tính số credit lấy từ từng nguồn
        double takeFromMonthly = Math.min(monthly, amount);

        double remaining = amount - takeFromMonthly;

        double takeFromPurchased = Math.min(purchased, remaining);

        // Không đủ credit
        if (takeFromMonthly + takeFromPurchased < amount) {
            throw new IllegalArgumentException(
                    "Số dư không đủ để thực hiện cuộc gọi AI"
            );
        }

        // Cập nhật số dư
        acc.setMonthlyQuotaRemaining(
                roundOneDecimal(monthly - takeFromMonthly)
        );

        acc.setPurchasedCreditBalance(
                roundOneDecimal(purchased - takeFromPurchased)
        );

        creditAccountRepository.save(acc);

        return new AccountDeduction(
                roundOneDecimal(takeFromMonthly),
                roundOneDecimal(takeFromPurchased),
                roundOneDecimal(takeFromMonthly + takeFromPurchased)
        );
    }


    @Transactional
    public void settleTransaction(
            String transactionId,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double actualCredit,
            String modelFromProvider // pass in model if you want to save it
    ) {
        CreditTransaction transaction = creditTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction không tồn tại"));


        if (transaction.getType() != CreditTransaction.TransactionType.HOLD) {
            throw new IllegalStateException(
                    "Transaction không ở trạng thái HOLD"
            );
        }
        
        // update usage tokens (safe cast)
        transaction.setPromptTokens(toDatabaseTokenCount(promptTokens));
        transaction.setCompletionTokens(toDatabaseTokenCount(completionTokens));
        transaction.setTotalTokens(toDatabaseTokenCount(totalTokens));

        double hold = transaction.getTotalCreditHold() != null
                    ? transaction.getTotalCreditHold()
                    : 0.0;

        if (actualCredit < 0) {
            throw new IllegalArgumentException(
                    "Actual credit không được âm"
            );
        }

        double difference = roundOneDecimal(hold - actualCredit);

        double refunded = Math.max(0.0, difference);

        double additionalDeduct = Math.max(0.0, -difference);

        if (actualCredit > hold) {
            LoggerFactory.getLogger(CreditService.class).warn(
                "AI actual credit vượt HOLD: transactionId={}, hold={}, actual={}, additionalDeduct={}",
                transaction.getTransactionId(),
                hold,
                actualCredit,
                additionalDeduct
            );
        }

        // Parse the HOLD_BREAKDOWN section from the transaction description and extract monthly and purchased deductions.
        HoldBreakdown breakdown =
                parseHoldBreakdown(transaction.getDescription());

        double monthlyTaken = breakdown.monthlyDeducted();
        double purchasedTaken = breakdown.purchasedDeducted();


        // Refund proportionally to original deduction
        double actualMonthlyUsed =
        Math.min(monthlyTaken, actualCredit);

        double remainingActual =
                actualCredit - actualMonthlyUsed;

        double actualPurchasedUsed =
                Math.min(purchasedTaken, remainingActual);

        double refundMonthly =
                roundOneDecimal(monthlyTaken - actualMonthlyUsed);

        double refundPurchased =
                roundOneDecimal(purchasedTaken - actualPurchasedUsed);

        refunded = roundOneDecimal(
                refundMonthly + refundPurchased
        );

        // Apply refunds back to account
        if (refundMonthly > 0 || refundPurchased > 0) {
            CreditAccount acc = creditAccountRepository
                    .findById(transaction.getUser().getUserId())
                    .orElseThrow(() ->
                            new RuntimeException("CreditAccount không tồn tại"));

            double currentMonthly =
                    acc.getMonthlyQuotaRemaining() != null
                            ? acc.getMonthlyQuotaRemaining()
                            : 0.0;

            double currentPurchased =
                    acc.getPurchasedCreditBalance() != null
                            ? acc.getPurchasedCreditBalance()
                            : 0.0;



            acc.setMonthlyQuotaRemaining(roundOneDecimal(currentMonthly + refundMonthly));
            acc.setPurchasedCreditBalance(roundOneDecimal(currentPurchased + refundPurchased));
            creditAccountRepository.save(acc);
        }
                       

        // update transaction fields
        transaction.setActualCreditDeducted(roundOneDecimal(actualCredit));
        transaction.setRefundedCredit(roundOneDecimal(refunded));
        transaction.setRefundedCredit(refunded);

        if (modelFromProvider != null) transaction.setModel(modelFromProvider);
        transaction.setType(CreditTransaction.TransactionType.DEDUCT);
        creditTransactionRepository.save(transaction);
    }

    // @Transactional
    // public void deductCredit(String transactionId, Double actualDeducted, Double refunded) {
    //     CreditTransaction transaction = creditTransactionRepository.findById(transactionId)
    //             .orElseThrow(() -> new RuntimeException("Transaction không tồn tại"));

    //     User user = userRepository.findById(transaction.getUser().getUserId())
    //             .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

    //     double currentBalance = user.getCreditBalance() != null ? user.getCreditBalance() : 0.0;
    //     user.setCreditBalance(currentBalance - (actualDeducted != null ? actualDeducted : 0.0));
    //     userRepository.save(user);

    //     transaction.setActualCreditDeducted(actualDeducted);
    //     transaction.setRefundedCredit(refunded);
    //     transaction.setType(CreditTransaction.TransactionType.DEDUCT);
    //     creditTransactionRepository.save(transaction);
    // }

    @Transactional
    public void refundHold(String transactionId) {
        CreditTransaction transaction = creditTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction không tồn tại"));
        if (transaction.getType() != CreditTransaction.TransactionType.HOLD) {
            return;
        }

        HoldBreakdown breakdown =
                parseHoldBreakdown(transaction.getDescription());

        double monthlyTaken = breakdown.monthlyDeducted();
        double purchasedTaken = breakdown.purchasedDeducted();

        // refund back
        CreditAccount acc = creditAccountRepository
                .findById(transaction.getUser().getUserId())
                .orElseThrow(() ->
                        new RuntimeException("CreditAccount không tồn tại"));

        double currentMonthly =
                acc.getMonthlyQuotaRemaining() != null
                        ? acc.getMonthlyQuotaRemaining()
                        : 0.0;

        double currentPurchased =
                acc.getPurchasedCreditBalance() != null
                        ? acc.getPurchasedCreditBalance()
                        : 0.0;

        acc.setMonthlyQuotaRemaining(
                roundOneDecimal(currentMonthly + monthlyTaken)
        );

        acc.setPurchasedCreditBalance(
                roundOneDecimal(currentPurchased + purchasedTaken)
        );

        creditAccountRepository.save(acc);

        transaction.setActualCreditDeducted(0.0);
        transaction.setRefundedCredit(roundOneDecimal(monthlyTaken + purchasedTaken));
        transaction.setType(CreditTransaction.TransactionType.REFUND);
        creditTransactionRepository.save(transaction);
    }

    // Convert token count to a value suitable for storing in the database, ensuring it is within the valid range.
    private int toDatabaseTokenCount(long tokens) {
    if (tokens <= 0) {
            return 0;
        }

        return (int) Math.min(Integer.MAX_VALUE, tokens);
    }

    // Parse the HOLD_BREAKDOWN section from the transaction description and return it as a HoldBreakdown object.
    private HoldBreakdown parseHoldBreakdown(String description) {
        String marker = "HOLD_BREAKDOWN:";

        if (description == null || !description.contains(marker)) {
            throw new IllegalStateException(
                    "Transaction không có HOLD_BREAKDOWN"
            );
        }

        String json = description
                .substring(description.indexOf(marker) + marker.length())
                .trim();

        try {
            JsonNode node = objectMapper.readTree(json);

            double monthlyDeducted = node.path("monthlyDeducted").asDouble(-1.0);
            double purchasedDeducted = node.path("purchasedDeducted").asDouble(-1.0);

            if (monthlyDeducted < 0 || purchasedDeducted < 0) {
                throw new IllegalStateException(
                        "Giá trị HOLD_BREAKDOWN không hợp lệ"
                );
            }

            return new HoldBreakdown(
                    monthlyDeducted,
                    purchasedDeducted
            );
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Không thể đọc HOLD_BREAKDOWN của transaction",
                    ex
            );
        }
    }

}
