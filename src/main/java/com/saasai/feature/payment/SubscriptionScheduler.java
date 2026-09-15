package com.saasai.feature.payment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Service
@RequiredArgsConstructor
public class SubscriptionScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final ResetPackageService resetPackageService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            // run the same maintenance once at startup
            processDailySubscriptionMaintenance();
        } catch (Exception ex) {
            logger.error("Failed to run daily subscription maintenance at startup: {}", ex.getMessage(), ex);
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void processDailySubscriptionMaintenance() {

        try {
            resetPackageService.processExpiredSubscriptions();
        } catch (Exception ex) {
            logger.error("processExpiredSubscriptions failed: {}", ex.getMessage(), ex);
        }

        try {
            resetPackageService.processMonthlyQuotaResets();
        } catch (Exception ex) {
            logger.error("processMonthlyQuotaResets failed: {}", ex.getMessage(), ex);
        }

        try {
            resetPackageService.processExpiredPurchasedCredits();
        } catch (Exception ex) {
            logger.error("processExpiredPurchasedCredits failed: {}", ex.getMessage(), ex);
        }
    }
}
