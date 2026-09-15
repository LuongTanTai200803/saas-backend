package com.saasai.service;

import com.saasai.repository.BillingInvoiceRepository;
import com.saasai.repository.CreditTransactionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.saasai.dto.DocumentDTO;
import com.saasai.dto.PaginatedResponseDTO;
import com.saasai.dto.UpdateUserProfileRequest;
import com.saasai.dto.UserProfileDTO;
import com.saasai.entity.BillingInvoice;
import com.saasai.entity.CreditTransaction;
import com.saasai.entity.FileMetadata;
import com.saasai.entity.User;
import com.saasai.repository.ChatSessionRepository;
import com.saasai.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;

import com.saasai.repository.FileMetadataRepository;

import com.saasai.dto.UserCreditSummaryDTO;
import com.saasai.feature.payment.BillingInvoiceDTO;
import com.saasai.feature.payment.CreditAccount;
import com.saasai.feature.payment.CreditAccountRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ChatSessionRepository chatSessionRepository;

        @Autowired
        private FileMetadataRepository fileUploadRepository;

        @Autowired
        private CreditAccountRepository creditAccountRepository;

        @Autowired
        private BillingInvoiceRepository billingInvoiceRepository;

        @Autowired 
        private CreditTransactionRepository creditTransactionRepository;


        public UserProfileDTO getUserProfileByEmail(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return UserProfileDTO.builder()
                                .userId(user.getUserId())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .agency(user.getAgency())
                                .phone(user.getPhone())
                                .position(user.getPosition())
                                .created_at(user.getCreatedAt())
                                .role(user.getRole().toString())
                                .packageType(user.getAdminPackageConfig() != null
                                                ? user.getAdminPackageConfig().toString()
                                                : null)
                                .expireDate(user.getExpireDate())
                                .affiliate(UserProfileDTO.AffiliateDTO.builder()
                                                .code(user.getAffiliateCode())
                                                .link(user.getAffiliateLink())
                                                .totalEarnings(user.getTotalEarnings())
                                                .build())
                                .build();
        }

        public UserProfileDTO getUserProfile(String userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return UserProfileDTO.builder()
                                .userId(user.getUserId())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .agency(user.getAgency())
                                .role(user.getRole().toString())
                                .packageType(user.getAdminPackageConfig() != null
                                                ? user.getAdminPackageConfig().toString()
                                                : null)
                                .expireDate(user.getExpireDate())
                                .affiliate(UserProfileDTO.AffiliateDTO.builder()
                                                .code(user.getAffiliateCode())
                                                .link(user.getAffiliateLink())
                                                .totalEarnings(user.getTotalEarnings())
                                                .build())
                                .build();
        }

        public User getUserById(String userId) {
                return userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
        }

        public PaginatedResponseDTO<DocumentDTO> getUserDocuments(String userId, int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                Page<com.saasai.entity.ChatSession> sessions = chatSessionRepository
                                .findByUser_UserIdOrderByUpdatedAtDesc(userId, pageable);

                return PaginatedResponseDTO.<DocumentDTO>builder()
                                .content(sessions.getContent().stream()
                                                .map(session -> DocumentDTO.builder()
                                                                .sessionId(session.getSessionId() != null
                                                                                ? String.valueOf(session.getSessionId())
                                                                                : null)
                                                                .sessionName(session.getSessionName())
                                                                .tagId(session.getTagId())
                                                                .updatedAt(session.getUpdatedAt())
                                                                .status(mapSessionStatus(session.getStatus()))
                                                                .build())
                                                .collect(Collectors.toList()))
                                .totalPages(sessions.getTotalPages())
                                .totalElements(sessions.getTotalElements())
                                .currentPage(page)
                                .pageSize(size)
                                .build();
        }

        public PaginatedResponseDTO<DocumentDTO> getUserDocumentsByUserEmail(String email, int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                Page<FileMetadata> files = fileUploadRepository.findByUserEmail(email, pageable);

                // Ép kiểu tường minh để tránh lỗi suy luận kiểu (type inference)
                List<DocumentDTO> documentList = files.getContent().stream()
                                .map(file -> DocumentDTO.builder()
                                                .sessionId(file.getFileId() != null
                                                                ? String.valueOf(file.getFileId())
                                                                : null)
                                                .sessionName(file.getFileName())
                                                .tagId(file.getCategory() != null
                                                                ? file.getCategory().name()
                                                                : null)
                                                .updatedAt(file.getUploadedAt())
                                                .status(file.getMimeType())
                                                .build())
                                .collect(Collectors.toList());

                return PaginatedResponseDTO.<DocumentDTO>builder()
                                .content(documentList)
                                .totalPages(files.getTotalPages())
                                .totalElements(files.getTotalElements())
                                .currentPage(page)
                                .pageSize(size)
                                .build();
        }

        // Lấy thông tin tóm tắt tín dụng của người dùng dựa trên email
        @Transactional(readOnly = true)
        public UserCreditSummaryDTO getUserCreditSummaryByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long aiCallCount = creditTransactionRepository.countByUser_UserIdAndType(
                user.getUserId(), CreditTransaction.TransactionType.DEDUCT);

        List<CreditAccount> accounts = creditAccountRepository.findByUser(user);
        CreditAccount account;
        if (accounts.isEmpty()) {
                account = CreditAccount.builder()
                        .user(user)
                        .userId(user.getUserId())
                        .monthlyQuotaAllocated(0.0)
                        .monthlyQuotaRemaining(0.0)
                        .purchasedCreditBalance(0.0)
                        .build();
                account = creditAccountRepository.save(account);
        } else {
                account = accounts.get(0);
        }

        return UserCreditSummaryDTO.builder()
                .userId(user.getUserId())
                .packageType(user.getAdminPackageConfig() != null
                        ? user.getAdminPackageConfig().getPackageType()
                        : null)
                .subscriptionExpireDate(user.getExpireDate())
                .monthly(UserCreditSummaryDTO.MonthlyCreditDTO.builder()
                        .allocated(account.getMonthlyQuotaAllocated() != null
                                ? account.getMonthlyQuotaAllocated()
                                : 0.0)
                        .remaining(account.getMonthlyQuotaRemaining() != null
                                ? account.getMonthlyQuotaRemaining()
                                : 0.0)
                        .cycleStart(account.getMonthlyQuotaCycleStart())
                        .cycleEnd(account.getMonthlyQuotaCycleEnd())
                        .build())
                .purchased(UserCreditSummaryDTO.PurchasedCreditDTO.builder()
                        .balance(account.getPurchasedCreditBalance() != null
                                ? account.getPurchasedCreditBalance()
                                : 0.0)
                        .purchasedAt(account.getPurchasedCreditPurchasedAt())
                        .expireAt(account.getPurchasedCreditExpireAt())
                        .build())
                .aiCallCount(aiCallCount)
                .build();
        }

        // helper mapper
        private BillingInvoiceDTO mapToBillingInvoiceDTO(BillingInvoice inv) {
        return BillingInvoiceDTO.builder()
                .invoiceId(inv.getInvoiceId())
                .userId(inv.getUser() != null ? inv.getUser().getUserId() : null)
                .packageType(inv.getAdminPackageConfig() != null ? inv.getAdminPackageConfig().getPackageType() : null)
                .durationMonths(inv.getDurationMonths())
                .memoId(inv.getMemoId())
                .originalAmount(inv.getOriginalAmount())
                .discountAmount(inv.getDiscountAmount())
                .finalAmount(inv.getFinalAmount())
                .qrCodeUrl(inv.getQrCodeUrl())
                .status(inv.getStatus() != null ? inv.getStatus().name() : null)
                .createdAt(inv.getCreatedAt())
                .paymentDate(inv.getPaymentDate())
                .build();
        }

        // Lấy hóa đơn cụ thể của người dùng dựa trên userId và invoiceId
        @Transactional(readOnly = true)
        public BillingInvoiceDTO getInvoiceForUser(String userId, String invoiceId) {
        BillingInvoice inv = billingInvoiceRepository
                .findByInvoiceIdAndUser_UserId(invoiceId, userId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        return mapToBillingInvoiceDTO(inv);
        }

        // Lấy danh sách hóa đơn của người dùng với phân trang
        @Transactional(readOnly = true)
        public PaginatedResponseDTO<BillingInvoiceDTO> getUserInvoices(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BillingInvoice> p = billingInvoiceRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);
        List<BillingInvoiceDTO> items = p.getContent().stream()
                .map(this::mapToBillingInvoiceDTO)
                .collect(Collectors.toList());

        return PaginatedResponseDTO.<BillingInvoiceDTO>builder()
                .content(items)
                .totalPages(p.getTotalPages())
                .totalElements(p.getTotalElements())
                .currentPage(page)
                .pageSize(size)
                .build();
        }
        // Cập nhật thông tin hồ sơ người dùng dựa trên email và yêu cầu cập nhật
        public UserProfileDTO updateUserProfileByEmail(String email, UpdateUserProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request == null) {
                throw new IllegalArgumentException("Request không được null");
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
                user.setFullName(request.getFullName());
        }

        if (request.getAgency() != null) {
                user.setAgency(request.getAgency().isBlank() ? null : request.getAgency());
        }

        if (request.getAffiliateCode() != null) {
                user.setAffiliateCode(request.getAffiliateCode().isBlank() ? null : request.getAffiliateCode());
        }

        if (request.getPhone() != null) {
                user.setPhone(request.getPhone().isBlank() ? null : request.getPhone());
        }
        if (request.getPosition() != null) {
                user.setPosition(request.getPosition().isBlank() ? null : request.getPosition());
        }
        
        user = userRepository.save(user);

        return UserProfileDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .agency(user.getAgency())
                .phone(user.getPhone())
                .position(user.getPosition())
                .created_at(user.getCreatedAt())
                .role(user.getRole() != null ? user.getRole().toString() : null)
                .packageType(user.getAdminPackageConfig() != null
                        ? user.getAdminPackageConfig().getPackageType()
                        : null)
                .expireDate(user.getExpireDate())
                .affiliate(UserProfileDTO.AffiliateDTO.builder()
                        .code(user.getAffiliateCode())
                        .link(user.getAffiliateLink())
                        .totalEarnings(user.getTotalEarnings())
                        .build())
                .build();
        }

        private String mapSessionStatus(com.saasai.entity.ChatSession.SessionStatus status) {
                if (status == null) {
                        return null;
                }

                return switch (status) {
                        case EDITING -> "Đang soạn thảo";
                        case DRAFT -> "Bản nháp";
                        case ACTIVE -> "Đang xử lý";
                        case COMPLETED -> "Hoàn thành";
                        case ARCHIVED -> "Đã lưu trữ";
                        default -> throw new IllegalArgumentException("Unexpected value: " + status);
                };
        }
}
