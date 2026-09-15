package com.saasai.admin;

import com.saasai.admin.AdminDashboardDTO;

import com.saasai.admin.UserAdminDTO;
import com.saasai.admin.UserUpdateRequest;
import com.saasai.admin.AdminPackageDTO;
import com.saasai.admin.AdminPackageUpdateDTO;
import com.saasai.admin.AdminService;
import com.saasai.repository.AdminPackageConfigRepository;
import com.saasai.repository.BillingInvoiceRepository;
import com.saasai.repository.TransactionRecordRepository;
import com.saasai.repository.UserRepository;
import com.saasai.repository.CreditTransactionRepository;
import com.saasai.repository.CreditTransactionRepository.TopAssistantProjection;
import com.saasai.repository.ChatSessionRepository;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.BillingInvoice;
import com.saasai.entity.CreditTransaction;
import com.saasai.entity.TransactionRecord;
import com.saasai.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saasai.feature.payment.CreditAccount;
import com.saasai.feature.payment.CreditAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private BillingInvoiceRepository billingInvoiceRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private AdminPackageConfigRepository adminPackageConfigRepository;

    @Autowired
    private CreditAccountRepository creditAccountRepository;

   

    // --- stubs for other AdminService methods (implement as needed) ---
    @Override
        @Transactional(readOnly = true)
        public AdminDashboardDTO getDashboardOverview() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);

        LocalDateTime startOfToday =
                today.atStartOfDay();

        LocalDateTime startOfTomorrow =
                today.plusDays(1).atStartOfDay();

        LocalDateTime startOfMonth =
                today.withDayOfMonth(1).atStartOfDay();

        LocalDateTime activeSince =
                LocalDateTime.now(zone).minusDays(30);

        long userCount = userRepository.count();

        // Count active users since the specified date (last 30 days)
        long activeUserCount =
                userRepository.countActiveUsersSince(activeSince);

        long transactionCount =
                transactionRecordRepository
                        .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                startOfMonth,
                                startOfTomorrow
                        );

        long aiUsageCount =
                creditTransactionRepository.countByTypeBetween(
                        CreditTransaction.TransactionType.DEDUCT,
                        startOfMonth,
                        startOfTomorrow
                );

        double creditsConsumed =
                creditTransactionRepository.sumActualCreditByTypeBetween(
                        CreditTransaction.TransactionType.DEDUCT,
                        startOfMonth,
                        startOfTomorrow
                );

        long revenueToday =
                billingInvoiceRepository.sumPaidAmountBetween(
                        startOfToday,
                        startOfTomorrow
                );

        long revenueMonth =
                billingInvoiceRepository.sumPaidAmountBetween(
                        startOfMonth,
                        startOfTomorrow
                );

        return new AdminDashboardDTO(
                userCount,
                activeUserCount,
                transactionCount,
                aiUsageCount,
                creditsConsumed,
                revenueToday,
                revenueMonth,
                today
        );
        }
    
    @Override
    @Transactional(readOnly = true)
        public UserAdminDTO getUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User không tồn tại: " + userId
                        ));

        return toUserAdminDTO(user);
        }
        
    @Override
        @Transactional
        public UserAdminDTO updateUser(
                String userId,
                UserUpdateRequest request
        ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User không tồn tại: " + userId
                        ));

        if (request.getPackageType() != null
                && !request.getPackageType().isBlank()) {
                AdminPackageConfig packageConfig =
                        adminPackageConfigRepository
                                .findByPackageType(request.getPackageType())
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Package không tồn tại: "
                                                        + request.getPackageType()
                                        ));

                user.setAdminPackageConfig(packageConfig);
        }

        if (request.getExpireDate() != null) {
                user.setExpireDate(request.getExpireDate());
        }

        userRepository.save(user);

        return toUserAdminDTO(user);
        }

    @Override
        @Transactional
        public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User không tồn tại: " + userId
                        ));

                if (user.getRole() == User.UserRole.ROLE_ADMIN) {
                throw new IllegalArgumentException(
                        "Không được xóa tài khoản admin"
                );
                }

        userRepository.delete(user);
        }

    @Override
        @Transactional(readOnly = true)
        public List<AdminPackageDTO> listPackages() {
        return adminPackageConfigRepository.findAll()
                .stream()
                .map(this::toAdminPackageDTO)
                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public AdminPackageDTO getPackage(String packageType) {
        return adminPackageConfigRepository
                .findByPackageType(packageType)
                .map(this::toAdminPackageDTO)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Package không tồn tại: " + packageType
                        ));
        }

        @Override
        public AdminPackageConfig getPackageConfig(String packageType) {
                return adminPackageConfigRepository.findByPackageType(packageType).orElse(null);
        }

    @Override
@Transactional
public AdminPackageDTO upsertPackageConfig(
        String packageType,
        AdminPackageUpdateDTO req
) {
    if (packageType == null || packageType.isBlank()) {
        throw new IllegalArgumentException(
                "packageType không được để trống"
        );
    }

    if (req == null) {
        throw new IllegalArgumentException(
                "Request không được null"
        );
    }

    AdminPackageConfig config =
            adminPackageConfigRepository
                    .findByPackageType(packageType.trim().toUpperCase())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Package không tồn tại: "
                                            + packageType
                            ));

    if (req.getPrice() != null) {
        if (req.getPrice() < 0) {
            throw new IllegalArgumentException(
                    "price không được âm"
            );
        }
        config.setPrice(req.getPrice());
    }

    if (req.getCreditLimit() != null) {
        if (req.getCreditLimit() < 0) {
            throw new IllegalArgumentException(
                    "creditLimit không được âm"
            );
        }
        config.setCreditLimit(req.getCreditLimit());
    }

    if (req.getDuration() != null) {
        if (req.getDuration() <= 0) {
            throw new IllegalArgumentException(
                    "duration phải lớn hơn 0"
            );
        }
        config.setDuration(req.getDuration());
    }

    if (req.getDescription() != null) {
        config.setDescription(req.getDescription());
    }

    if (req.getStorageQuotaMb() != null) {
        if (req.getStorageQuotaMb() < 0) {
            throw new IllegalArgumentException(
                    "storageQuotaMb không được âm"
            );
        }
        config.setStorageQuotaMb(req.getStorageQuotaMb());
    }

    return toAdminPackageDTO(
            adminPackageConfigRepository.save(config)
    );
}
    // Lists users with pagination, converting them to UserAdminDTOs.
    @Override    
    @Transactional(readOnly = true)
        public List<UserAdminDTO> listUsers(Integer page, Integer size) {
        int pageNumber = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        return userRepository.findAll(pageable)
                .getContent()
                .stream()
                .map(this::toUserAdminDTO)
                .toList();
        }


    // Resolves the status of a user based on their last login date. Returns "ACTIVE" or "INACTIVE".
    private String resolveUserStatus(User user) {
        if (user.getLastLoginAt() == null) {
                return "INACTIVE";
        }

        if (user.getLastLoginAt()
                .isBefore(LocalDateTime.now().minusDays(30))) {
                return "INACTIVE";
        }

        return "ACTIVE";
        }

    // Converts a User entity to a UserAdminDTO, including calculating credits and resolving status.
    private UserAdminDTO toUserAdminDTO(User user) {
        Double credits = creditAccountRepository.findById(user.getUserId())
                .map(account -> {
                        double monthly = account.getMonthlyQuotaRemaining() == null
                                ? 0.0
                                : account.getMonthlyQuotaRemaining();

                        double purchased = account.getPurchasedCreditBalance() == null
                                ? 0.0
                                : account.getPurchasedCreditBalance();

                        return monthly + purchased;
                })
                .orElse(0.0);

        String status = resolveUserStatus(user);

        return new UserAdminDTO(
                user.getUserId(),
                user.getEmail(),
                user.getAdminPackageConfig() != null
                        ? user.getAdminPackageConfig().getPackageType()
                        : null,
                user.getExpireDate(),
                credits,
                status
        );
        }

        @Override
        @Transactional(readOnly = true)
        public List<UserPaymentHistoryDTO> listUserPayments(String userId) {
        if (!userRepository.existsById(userId)) {
                throw new IllegalArgumentException(
                        "User không tồn tại: " + userId
                );
        }

        return billingInvoiceRepository
                .findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(invoice -> new UserPaymentHistoryDTO(
                        invoice.getInvoiceId(),
                        invoice.getAdminPackageConfig() != null
                                ? invoice.getAdminPackageConfig()
                                        .getPackageType()
                                : null,
                        invoice.getFinalAmount(),
                        invoice.getStatus() != null
                                ? invoice.getStatus().name()
                                : null,
                        invoice.getMemoId(),
                        invoice.getCreatedAt(),
                        invoice.getPaymentDate()
                ))
                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<UserAiUsageDTO> listUserAiUsage(String userId) {
        if (!userRepository.existsById(userId)) {
                throw new IllegalArgumentException(
                        "User không tồn tại: " + userId
                );
        }

        return creditTransactionRepository
                .findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(transaction -> new UserAiUsageDTO(
                        transaction.getTransactionId(),
                        transaction.getModel(),
                        transaction.getModelPackageId(),
                        transaction.getPromptTokens(),
                        transaction.getCompletionTokens(),
                        transaction.getTotalTokens(),
                        transaction.getActualCreditDeducted(),
                        transaction.getRefundedCredit(),
                        transaction.getType() != null
                                ? transaction.getType().name()
                                : null,
                        transaction.getCreatedAt()
                ))
                .toList();
        }

        private AdminPackageDTO toAdminPackageDTO(
        AdminPackageConfig config
                ) {
                return new AdminPackageDTO(
                        config.getId(),
                        config.getPackageType(),
                        config.getPackageCategory() != null
                                ? config.getPackageCategory().name()
                                : null,
                        config.getPrice(),
                        config.getCreditLimit(),
                        config.getDuration(),
                        config.getDescription(),
                        config.getStorageQuotaMb()
                        
                  
                );
                }

                // Lấy doanh thu và số lượng hóa đơn đã thanh toán trong khoảng thời gian được chỉ định
        @Override
        @Transactional(readOnly = true)
        public AdminRevenueDTO getRevenue(DashboardRange range) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

        LocalDateTime from = range.dashboardStart(zone);
        LocalDateTime to = range.dashboardEnd(zone);

        long totalRevenue =
                billingInvoiceRepository.sumPaidAmountBetween(from, to);

        long paidInvoiceCount =
                billingInvoiceRepository.countPaidBetween(from, to);

        return new AdminRevenueDTO(
                range.apiValue(),
                from,
                to,
                totalRevenue,
                paidInvoiceCount,
                buildDashboardPoints(from, to)
        );
        }

        @Override
        @Transactional(readOnly = true)
        public AdminAiUsageDTO getAiUsage(DashboardRange range) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

        LocalDateTime from = range.start(zone);
        LocalDateTime to = range.end(zone);

        CreditTransaction.TransactionType type =
                CreditTransaction.TransactionType.DEDUCT;

        long transactionCount =
                creditTransactionRepository.countByTypeBetween(
                        type,
                        from,
                        to
                );

        long promptTokens =
                creditTransactionRepository.sumPromptTokensByTypeBetween(
                        type,
                        from,
                        to
                );

        long completionTokens =
                creditTransactionRepository.sumCompletionTokensByTypeBetween(
                        type,
                        from,
                        to
                );

        long totalTokens =
                creditTransactionRepository.sumTotalTokensByTypeBetween(
                        type,
                        from,
                        to
                );

        double creditsConsumed =
                creditTransactionRepository.sumActualCreditByTypeBetween(
                        type,
                        from,
                        to
                );

        return new AdminAiUsageDTO(
                range.apiValue(),
                from,
                to,
                transactionCount,
                promptTokens,
                completionTokens,
                totalTokens,
                creditsConsumed
        );
        }
        
        // Builds the dashboard points for the admin dashboard within the specified date range.
        private List<AdminDashboardPointDTO> buildDashboardPoints(
        LocalDateTime from,
        LocalDateTime to
        ) {
        Map<String, Long> revenueByDay = new HashMap<>();

        billingInvoiceRepository
                .sumPaidAmountByDay(from, to)
                .forEach(point ->
                        revenueByDay.put(
                                point.getLabel(),
                                point.getRevenue() == null
                                        ? 0L
                                        : point.getRevenue()
                        )
                );

        Map<String, Long> tokensByDay = new HashMap<>();

        creditTransactionRepository
                .sumTotalTokensByDay(from, to)
                .forEach(point ->
                        tokensByDay.put(
                                point.getLabel(),
                                point.getTotalTokens() == null
                                        ? 0L
                                        : point.getTotalTokens()
                        )
                );

        Set<String> labels = new TreeSet<>();
        labels.addAll(revenueByDay.keySet());
        labels.addAll(tokensByDay.keySet());

        return labels.stream()
                .map(label -> new AdminDashboardPointDTO(
                        label,
                        revenueByDay.getOrDefault(label, 0L),
                        tokensByDay.getOrDefault(label, 0L)
                ))
                .toList();
        }
        @Override
        @Transactional(readOnly = true)
        public List<AdminTopAssistantDTO> getTopAssistants(
                DashboardRange range
        ) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

        LocalDateTime from = range.dashboardStart(zone);
        LocalDateTime to = range.dashboardEnd(zone);

        List<TopAssistantProjection> rows =
                creditTransactionRepository.findTopAssistants(
                        from,
                        to,
                        PageRequest.of(0, 10)
                );

        long totalTokens = rows.stream()
                .mapToLong(row ->
                        row.getTotalTokens() == null
                                ? 0L
                                : row.getTotalTokens()
                )
                .sum();

        return rows.stream()
                .map(row -> {
                        long rowTokens = row.getTotalTokens() == null
                                ? 0L
                                : row.getTotalTokens();

                        double usagePercent = totalTokens <= 0
                                ? 0.0
                                : Math.round(
                                        rowTokens * 10000.0 / totalTokens
                                ) / 100.0;

                        return new AdminTopAssistantDTO(
                                row.getAssistantId(),
                                row.getName(),
                                rowTokens,
                                row.getCreditsConsumed() == null
                                        ? 0.0
                                        : row.getCreditsConsumed(),
                                usagePercent
                        );
                })
                .toList();
        }
}