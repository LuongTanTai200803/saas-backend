package com.saasai.feature.payment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.User;
import com.saasai.repository.AdminPackageConfigRepository;
import com.saasai.repository.UserRepository;
import com.saasai.feature.payment.CreditAccountRepository;
import com.saasai.feature.payment.CreditAccount;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class ResetPackageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResetPackageService.class);

    private final MonthlyQuotaPolicy monthlyQuotaPolicy;
    private final AdminPackageConfigRepository packageRepository;
    private final UserRepository userRepository;
    private final CreditAccountRepository creditAccountRepository;

    public ResetPackageService( MonthlyQuotaPolicy monthlyQuotaPolicy,
                               AdminPackageConfigRepository packageRepository,
                               UserRepository userRepository,
                               CreditAccountRepository creditAccountRepository) {
        this.monthlyQuotaPolicy = monthlyQuotaPolicy;
        this.packageRepository = packageRepository;
        this.userRepository = userRepository;
        this.creditAccountRepository = creditAccountRepository;
    }

    /**
     * Chạy hàng ngày: kiểm tra user hết hạn subscription -> chuyển sang FREE và clear monthly quota.
     */
    @Transactional
    public void processExpiredSubscriptions() {
        List<User> users = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        AdminPackageConfig freePackage = packageRepository
                .findByPackageType("FREE")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy FREE package"));
                    
        for (User user : users) {
            try {
                if (user.getExpireDate() == null || now.isBefore(user.getExpireDate())) {
                    continue;
                }

                // Đã là FREE thì không cần xử lý
                if (user.getAdminPackageConfig() != null
                        && "FREE".equalsIgnoreCase(user.getAdminPackageConfig().getPackageType())) {
                    continue;
                }

                logger.info("Expiring subscription: user={}, expireDate={}", user.getUserId(), user.getExpireDate());

                user.setAdminPackageConfig(freePackage);
                user.setExpireDate(null);
                userRepository.save(user);

                CreditAccount acc = creditAccountRepository.findById(user.getUserId()).orElse(null);

                if (acc != null) {
                    double freeQuota = freePackage.getCreditLimit() != null ? freePackage.getCreditLimit() : 0.0;

                    acc.setMonthlyQuotaAllocated(freeQuota);
                    acc.setMonthlyQuotaRemaining(freeQuota);
                    acc.setMonthlyQuotaCycleStart(null);
                    acc.setMonthlyQuotaCycleEnd(null);

                    creditAccountRepository.save(acc);
                }
            } catch (Exception ex) {
                logger.warn("Failed to process expiration for user {}: {}", user.getUserId(), ex.getMessage(), ex);
                continue;
            }
        }
    }

    /**
     * Chạy hàng ngày: cho tất cả user, gọi policy reset quota nếu đến chu kỳ.
     */
    @Transactional
    public void processMonthlyQuotaResets() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                monthlyQuotaPolicy.resetMonthlyQuotaIfNeeded(user);
            } catch (Exception ex) {
                logger.warn("Failed to reset monthly quota for user {}: {}", user.getUserId(), ex.getMessage());
            }
        }
    }

    /**
     * Chạy hàng ngày: cho tất cả user, gọi policy expire purchased credit nếu hết hạn.
     */
    @Transactional
    public void processExpiredPurchasedCredits() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        int count = creditAccountRepository.expirePurchasedCredits(now);

        logger.info("Expired {} purchased credit accounts", count);
    }


}