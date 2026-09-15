package com.saasai.feature.payment;



import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.User;

public interface CreditAllocationPolicy {
    void allocateSubscriptionCredits(User user, AdminPackageConfig pkg);
    void allocateCreditPack(User user, double credits, int durationDays);
    void resetMonthlyQuotaIfNeeded(User user );
}
