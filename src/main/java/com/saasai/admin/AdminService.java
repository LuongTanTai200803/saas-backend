package com.saasai.admin;

import com.saasai.admin.*;

import java.util.List;

public interface AdminService {
    AdminDashboardDTO getDashboardOverview();

    List<UserAdminDTO> listUsers(Integer page, Integer size);
    UserAdminDTO getUser(String userId);
    UserAdminDTO updateUser(String userId, UserUpdateRequest req);
    void deleteUser(String userId);

    // User payment history
    List<UserPaymentHistoryDTO> listUserPayments(String userId);

    // User AI usage history
    List<UserAiUsageDTO> listUserAiUsage(String userId);

    List<AdminPackageDTO> listPackages();
        // add this line to the interface
    com.saasai.entity.AdminPackageConfig getPackageConfig(String packageType);
    AdminPackageDTO getPackage(String packageType);
    
    // Upsert (update or insert) a package configuration
    AdminPackageDTO upsertPackageConfig(String packageType, AdminPackageUpdateDTO req);

    AdminRevenueDTO getRevenue(DashboardRange range);

    AdminAiUsageDTO getAiUsage(DashboardRange range);
    List<AdminTopAssistantDTO> getTopAssistants(
            DashboardRange range
    );
}