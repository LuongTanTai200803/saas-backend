package com.saasai.service;

import com.saasai.dto.AuthResponseDTO;
import com.saasai.dto.LoginRequestDTO;
import com.saasai.dto.RegisterRequestDTO;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.RefreshToken;
import com.saasai.entity.User;
import com.saasai.entity.User.UserRole;
import jakarta.persistence.EntityManager;
import com.saasai.exception.AuthException;
import com.saasai.feature.payment.MonthlyQuotaPolicy;
import com.saasai.feature.payment.ResetPackageService;
import com.saasai.repository.AdminPackageConfigRepository;
import com.saasai.repository.UserRepository;
import com.saasai.security.JwtTokenProvider;
import com.saasai.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ResetPackageService resetPackageService;

    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private AdminPackageConfigRepository adminPackageConfigRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private MonthlyQuotaPolicy monthlyQuotaPolicy;

    @Transactional
    public void registerUser(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email này đã được đăng ký trong hệ thống!");
        }
        AdminPackageConfig freePackage = adminPackageConfigRepository.findByPackageType("FREE")
                    .orElseThrow(() -> new RuntimeException("Gói FREE chưa được khởi tạo dưới DB!"));
        
        String encodedPassword =
        passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .fullName(request.getEmail().split("@")[0])
                .agency("")
                .role(User.UserRole.ROLE_USER)
                .adminPackageConfig(freePackage) // Gán gói FREE mặc định
                .expireDate(null) // Không đặt ngày hết hạn cho gói FREE
                .build();

        user = userRepository.save(user);

        System.out.println(
                "[REGISTER] User created successfully: userId="
                        + user.getUserId()
                        + ", email="
                        + user.getEmail()
        );

        if (user.getUserId() == null) {
            throw new IllegalStateException(
                    "User chưa được sinh userId sau khi save"
            );
        }

            monthlyQuotaPolicy.allocateSubscriptionCredits(user, freePackage);
        ;
    }

    public AuthResponseDTO loginUser(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Email hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(
                "Email hoặc mật khẩu không chính xác", 
                HttpStatus.UNAUTHORIZED);
        }
        // Cập nhật thời gian đăng nhập cuối cùng
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);


        String role = user.getRole() != null ? user.getRole().toString() : "ROLE_USER";
        String accessToken = tokenProvider.generateAccessToken(user.getUserId(), user.getEmail(), role);
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        
        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(role)
                .build();
    }

    public String refreshAccessToken(String refreshToken) {
        RefreshToken tokenEntity = refreshTokenService.validateRefreshToken(refreshToken);
        User user = tokenEntity.getUser();
        if (user == null) {
            throw new AuthException("Người dùng không tồn tại", HttpStatus.UNAUTHORIZED);
        }

        String userRoleElement = user.getRole() != null ? user.getRole().toString() : UserRole.ROLE_USER.toString();
        return tokenProvider.generateAccessToken(user.getUserId(), user.getEmail(), userRoleElement);
    }
    
    // Thêm logic verify Google ID token và cấp JWT nội bộ
    @Transactional
    public AuthResponseDTO loginWithGoogle(String googleIdToken) {
    if (googleIdToken == null || googleIdToken.trim().isEmpty()) {
        throw new RuntimeException("Google ID Token không được để trống");
    }

        try {
            // Verify Google ID token
            Map<String, String> googleUserInfo = tokenProvider.verifyGoogleIdToken(googleIdToken);
            
            String email = googleUserInfo.get("email");
            String fullName = googleUserInfo.get("name");
            String avatarUrl = googleUserInfo.get("picture");
            String providerId = googleUserInfo.get("sub");

            if (email == null || email.isBlank()) {
                throw new RuntimeException("Không thể lấy email từ Google token");
            }

            // Tìm user cũ
            Optional<User> existingUserOpt = userRepository.findByEmail(email);
            User user;

            if (existingUserOpt.isPresent()) {
                user = existingUserOpt.get();
                // Cập nhật thông tin mới nhất từ Google
                user.setFullName(fullName);
                user.setAvatarUrl(avatarUrl);
                user.setProvider("GOOGLE");
                user.setProviderId(providerId);
                System.out.println("[AuthService] User Google tồn tại, cập nhật: " + email);
            } else {
                // Tạo user mới
                AdminPackageConfig freePackage = adminPackageConfigRepository.findByPackageType("FREE")
                        .orElseThrow(() -> new RuntimeException("Gói FREE chưa được khởi tạo trong DB!"));

                user = User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("GOOGLE_" + UUID.randomUUID())) // random password an toàn
                        .fullName(fullName != null ? fullName : email)
                        .agency("")
                        .role(User.UserRole.ROLE_USER)
                        .adminPackageConfig(freePackage)
                        .expireDate(null)
                        .avatarUrl(avatarUrl)
                        .provider("GOOGLE")
                        .providerId(providerId)
                        .build();

                user = userRepository.save(user);
                monthlyQuotaPolicy.allocateSubscriptionCredits(user, freePackage);
                System.out.println("[AuthService] Tạo user mới từ Google: " + email);
            }

            // Tạo token
            String role = user.getRole() != null ? user.getRole().toString() : "ROLE_USER";
            // Cập nhật thời gian đăng nhập cuối cùng
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            String accessToken = tokenProvider.generateAccessToken(user.getUserId(), user.getEmail(), role);
            String refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

            return AuthResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .role(role)
                    .build();

        } catch (Exception e) {
            System.err.println("[AuthService] Lỗi loginWithGoogle: " + e.getMessage());
            if (e.getMessage().toLowerCase().contains("expired") || e.getMessage().toLowerCase().contains("invalid")) {
                throw new RuntimeException("Token Google đã hết hạn hoặc không hợp lệ. Vui lòng thử lại.");
            }
            throw new RuntimeException("Đăng nhập Google thất bại: " + e.getMessage());
        }
    }

}