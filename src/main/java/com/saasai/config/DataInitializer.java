package com.saasai.config;

import com.saasai.entity.User;
import com.saasai.feature.payment.MonthlyQuotaPolicy;
import com.saasai.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import com.saasai.entity.AdminPackageConfig;
import com.saasai.repository.AdminPackageConfigRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    private final AdminPackageConfigRepository adminPackageConfigRepository;

    private final PasswordEncoder passwordEncoder; // Lôi bộ mã hóa chính chủ ra dùng
    
    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Autowired
    private MonthlyQuotaPolicy monthlyQuotaPolicy;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {

            AdminPackageConfig freePackage = adminPackageConfigRepository.findByPackageType("FREE")
                    .orElseThrow(() -> new RuntimeException("Gói FREE chưa được khởi tạo dưới DB!"));

            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .fullName(adminName)
                    .agency("Ban Quản Trị Hệ Thống")
                    .role(User.UserRole.ROLE_ADMIN)
                    .adminPackageConfig(freePackage)
                    .provider("KHÔNG CÓ")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            admin = userRepository.save(admin);
            monthlyQuotaPolicy.allocateSubscriptionCredits(admin, freePackage);

            System.out.println("🚀 [DEV PROFILE] Khởi tạo thành công tài khoản mồi Admin: " + adminEmail);
        } else {
            System.out.println("ℹ️ [DEV PROFILE] Tài khoản Admin đã tồn tại từ trước, bỏ qua bước khởi tạo mồi.");
        }
    }
}