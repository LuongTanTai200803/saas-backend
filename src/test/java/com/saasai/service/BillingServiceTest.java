package com.saasai.service;

import com.saasai.admin.AdminService;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.BillingInvoice;
import com.saasai.entity.CreditTransaction;
import com.saasai.entity.User;
import com.saasai.feature.payment.BillingService;
import com.saasai.feature.payment.CreditAccount;
import com.saasai.feature.payment.MonthlyQuotaPolicy;
import com.saasai.repository.BillingInvoiceRepository;
import com.saasai.repository.CreditTransactionRepository;
import com.saasai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BillingServiceTest {
    @Mock
    private BillingInvoiceRepository billingInvoiceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminService adminService;

    @Mock
    private CreditTransactionRepository creditTransactionRepository;

    @InjectMocks
    private BillingService billingService;

    @Mock
    private MonthlyQuotaPolicy monthlyQuotaPolicy;

    private final String testUserId = "user-uuid-10293"; // 🎯 ĐÃ SỬA
    private final String testInvoiceId = "invoice-uuid-100"; // 🎯 ĐÃ SỬA

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processPaidInvoice_shouldApplyCreditsAndExpiryOnce() {
        AdminPackageConfig config = AdminPackageConfig.builder()
                .packageType("BASIC") // 🎯 ĐÃ SỬA: Dùng String thay vì Enum cồng kềnh
                .creditLimit(100.0)
                .storageQuotaMb(100L)
                .price(199000L)
                .build();

        User user = User.builder()
                .userId(testUserId) // 🎯 ĐÃ SỬA
                .expireDate(LocalDateTime.now().minusDays(1))
                .adminPackageConfig(config) // 🎯 ĐÃ SỬA: Nạp Object Config liên kết ngoại
                .build();

        CreditAccount account = CreditAccount.builder()
        .userId(testUserId)
        .user(user)
        .monthlyQuotaRemaining(10.0)
        .monthlyQuotaAllocated(10.0)
        .purchasedCreditBalance(0.0)
        .build();

        user.setCreditAccount(account);

        BillingInvoice invoice = BillingInvoice.builder()
                .invoiceId(testInvoiceId) // 🎯 ĐÃ SỬA
                .user(user) // 🎯 ĐÃ SỬA: Nạp nguyên Object liên kết hướng đối tượng
                .adminPackageConfig(config) // 🎯 ĐÃ SỬA
                .durationMonths(2)
                .status(BillingInvoice.InvoiceStatus.PAID) // 🎯 ĐÃ SỬA
                .build();


        when(billingInvoiceRepository.findById(testInvoiceId)).thenReturn(Optional.of(invoice));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
        when(adminService.getPackageConfig("BASIC")).thenReturn(config);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billingInvoiceRepository.save(any(BillingInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(creditTransactionRepository.save(any(CreditTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        billingService.processPaidInvoice(testInvoiceId);

        assertThat(user.getExpireDate()).isAfter(LocalDateTime.now().plusDays(59));
        assertThat(account.getMonthlyQuotaRemaining())
        .isEqualTo(100.0);

        verify(creditTransactionRepository, times(1)).save(any(CreditTransaction.class));

        // Tái lập tính Idempotency
        billingService.processPaidInvoice(testInvoiceId);
        
        verify(monthlyQuotaPolicy)
        .allocateSubscriptionCredits(user, config);
        verify(creditTransactionRepository, times(1)).save(any(CreditTransaction.class));
    }
}