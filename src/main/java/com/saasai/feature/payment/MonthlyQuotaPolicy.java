package com.saasai.feature.payment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.saasai.entity.*;
import com.saasai.feature.payment.CreditAccountRepository;
import com.saasai.feature.payment.CreditAccount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class MonthlyQuotaPolicy implements CreditAllocationPolicy {
        @PersistenceContext
        private EntityManager entityManager;

    private final CreditAccountRepository creditAccountRepository;

    public MonthlyQuotaPolicy(CreditAccountRepository creditAccountRepository) { this.creditAccountRepository = creditAccountRepository; }

    @Override
    @Transactional
        public void allocateSubscriptionCredits(
                User user,
                AdminPackageConfig pkg
        ) {


        if (pkg == null) {
                throw new IllegalArgumentException("Package không được null");
        }

        LocalDateTime now =
                LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        double monthly = pkg.getCreditLimit() == null
                ? 0.0
                : pkg.getCreditLimit();

        CreditAccount account = getOrCreateAccount(user);

        account.setMonthlyQuotaAllocated(monthly);
        account.setMonthlyQuotaRemaining(monthly);
        account.setMonthlyQuotaCycleStart(now);
        account.setMonthlyQuotaCycleEnd(now.plusMonths(1));

        creditAccountRepository.save(account);
        }

    @Override
    @Transactional
        public void allocateCreditPack(
                User user,
                double credits,
                int durationDays
        ) {
        if (credits <= 0) {
                throw new IllegalArgumentException(
                        "Số credit phải lớn hơn 0"
                );
        }

        if (durationDays <= 0) {
                throw new IllegalArgumentException(
                        "Thời hạn credit pack phải lớn hơn 0 ngày"
                );
        }

        CreditAccount account = getOrCreateAccount(user);

        double currentBalance =
                account.getPurchasedCreditBalance() == null
                        ? 0.0
                        : account.getPurchasedCreditBalance();

        account.setPurchasedCreditBalance(
                currentBalance + credits
        );

        LocalDateTime now =
                LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        account.setPurchasedCreditPurchasedAt(now);
        account.setPurchasedCreditExpireAt(
                now.plusDays(durationDays)
        );
        }

    @Override
    @Transactional
    public void resetMonthlyQuotaIfNeeded(User user) {
        if (user == null) return;

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        CreditAccount acc =
                    creditAccountRepository.findById(user.getUserId())
                            .orElse(null);

        if (acc == null) {
            return;
        }
        // Chưa hết chu kỳ quota
        if (acc.getMonthlyQuotaCycleEnd() != null
                && now.isBefore(acc.getMonthlyQuotaCycleEnd())) {
            return;
        }

        AdminPackageConfig pkg = user.getAdminPackageConfig();

        // Subscription hết hạn → không cấp quota mới
        if (user.getExpireDate() == null
                || !now.isBefore(user.getExpireDate())) {
            acc.setMonthlyQuotaRemaining(pkg.getCreditLimit() != null ? pkg.getCreditLimit() : 0.0);
            creditAccountRepository.save(acc);
            return;
        }

        // Cấp lại quota mới dựa trên credit limit của gói subscription
        double monthly = pkg.getCreditLimit();

        // Reset quota tháng
        acc.setMonthlyQuotaAllocated(monthly);
        acc.setMonthlyQuotaRemaining(monthly);

        acc.setMonthlyQuotaCycleStart(acc.getMonthlyQuotaCycleEnd() != null
                ? acc.getMonthlyQuotaCycleEnd()
                : acc.getMonthlyQuotaCycleEnd());
                
        acc.setMonthlyQuotaCycleEnd(acc.getMonthlyQuotaCycleEnd().plusMonths(1));

        creditAccountRepository.save(acc);
    }

    // Lấy CreditAccount của user, nếu chưa tồn tại thì tạo mới.
    private CreditAccount getOrCreateAccount(User user) {

    if (user == null || user.getUserId() == null) {
        throw new IllegalArgumentException(
                "User phải được lưu trước khi tạo CreditAccount"
        );
    }

    User managedUser = entityManager.find(
            User.class,
            user.getUserId()
    );

    if (managedUser == null) {
        throw new IllegalArgumentException(
                "Không tìm thấy User: " + user.getUserId()
        );
    }

    CreditAccount account = creditAccountRepository
            .findById(managedUser.getUserId())
            .orElse(null);

    if (account == null) {

        System.out.println(
                "[CREDIT] Creating CreditAccount for userId="
                        + managedUser.getUserId()
        );

        account = CreditAccount.builder()
                .userId(managedUser.getUserId())
                .user(managedUser)
                .monthlyQuotaAllocated(0.0)
                .monthlyQuotaRemaining(0.0)
                .purchasedCreditBalance(0.0)
                .build();

        entityManager.persist(account);
        entityManager.flush();

    } else {

        account.setUser(managedUser);
        account.setUserId(managedUser.getUserId());
    }

    // Quan trọng: cập nhật quan hệ 2 chiều
    managedUser.setCreditAccount(account);

    return account;
}
}
