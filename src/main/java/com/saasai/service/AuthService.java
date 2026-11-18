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

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .fullName(request.getEmail().split("@")[0])
                .agency("")
                .role(User.UserRole.ROLE_USER)
                .adminPackageConfig(freePackage) // Gán gói FREE mặc định
                .expireDate(null) // Không đặt ngày hết hạn cho gói FREE
                .provider("LOCAL")
                .build();

        user = userRepository.save(user);

        System.out.println(
                "[REGISTER] User created successfully: userId="
                        + user.getUserId()
                        + ", email="
                        + user.getEmail());

        if (user.getUserId() == null) {
            throw new IllegalStateException(
                    "User chưa được sinh userId sau khi save");
        }

        monthlyQuotaPolicy.allocateSubscriptionCredits(user, freePackage);
        ;
    }

    public AuthResponseDTO loginUser(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Email hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED));

        // Kiểm tra mật khẩu
        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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
        if (googleIdToken == null || googleIdToken.isBlank()) {
            throw new AuthException("Google ID Token không được để trống", HttpStatus.BAD_REQUEST);
        }

        Map<String, String> googleUserInfo = tokenProvider.verifyGoogleIdToken(googleIdToken);

        String email = googleUserInfo.get("email");
        String providerId = googleUserInfo.get("sub");
        String emailVerified = googleUserInfo.get("email_verified");

        if (email == null || email.isBlank() || providerId == null || providerId.isBlank()) {
            throw new AuthException("Google token thiếu thông tin tài khoản", HttpStatus.UNAUTHORIZED);
        }

        if (!"true".equalsIgnoreCase(emailVerified)) {
            throw new AuthException("Email Google chưa được xác minh", HttpStatus.UNAUTHORIZED);
        }

        email = email.trim().toLowerCase(java.util.Locale.ROOT);

        User user = userRepository
                .findByProviderAndProviderId("GOOGLE", providerId)
                .orElse(null);

        if (user == null) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new AuthException(
                        "Email này đã có tài khoản. Hãy đăng nhập tài khoản hiện tại trước khi liên kết Google.",
                        HttpStatus.CONFLICT);
            }

            AdminPackageConfig freePackage = adminPackageConfigRepository
                    .findByPackageType("FREE")
                    .orElseThrow(() -> new IllegalStateException("Gói FREE chưa được khởi tạo trong DB"));

            user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("GOOGLE_" + UUID.randomUUID()))
                    .fullName(googleUserInfo.getOrDefault("name", email))
                    .agency("")
                    .role(UserRole.ROLE_USER)
                    .adminPackageConfig(freePackage)
                    .expireDate(null)
                    .avatarUrl(googleUserInfo.get("picture"))
                    .provider("GOOGLE")
                    .providerId(providerId)
                    .build();

            user = userRepository.save(user);
            monthlyQuotaPolicy.allocateSubscriptionCredits(user, freePackage);
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String role = user.getRole() != null
                ? user.getRole().toString()
                : UserRole.ROLE_USER.toString();

        return AuthResponseDTO.builder()
                .accessToken(tokenProvider.generateAccessToken(
                        user.getUserId(), user.getEmail(), role))
                .refreshToken(refreshTokenService.createRefreshToken(user.getEmail()))
                .role(role)
                .build();
    }

    @Transactional
    public void linkGoogleAccount(String currentEmail, String googleIdToken) {
        if (googleIdToken == null || googleIdToken.isBlank()) {
            throw new AuthException("Google ID Token không được để trống", HttpStatus.BAD_REQUEST);
        }

        Map<String, String> googleUserInfo = tokenProvider.verifyGoogleIdToken(googleIdToken);

        String googleEmail = googleUserInfo.get("email");
        String providerId = googleUserInfo.get("sub");
        String emailVerified = googleUserInfo.get("email_verified");

        if (googleEmail == null || googleEmail.isBlank()
                || providerId == null || providerId.isBlank()) {
            throw new AuthException("Google token thiếu thông tin tài khoản", HttpStatus.UNAUTHORIZED);
        }

        if (!"true".equalsIgnoreCase(emailVerified)) {
            throw new AuthException("Email Google chưa được xác minh", HttpStatus.UNAUTHORIZED);
        }

        String normalizedGoogleEmail = googleEmail.trim().toLowerCase(java.util.Locale.ROOT);
        String normalizedCurrentEmail = currentEmail.trim().toLowerCase(java.util.Locale.ROOT);

        if (!normalizedGoogleEmail.equals(normalizedCurrentEmail)) {
            throw new AuthException(
                    "Email Google phải trùng với email của tài khoản đang đăng nhập.",
                    HttpStatus.CONFLICT);
        }

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new AuthException(
                        "Không tìm thấy tài khoản đang đăng nhập",
                        HttpStatus.UNAUTHORIZED));

        Optional<User> googleOwner = userRepository.findByProviderAndProviderId("GOOGLE", providerId);

        if (googleOwner.isPresent()
                && !googleOwner.get().getUserId().equals(user.getUserId())) {
            throw new AuthException(
                    "Tài khoản Google này đã được liên kết với tài khoản khác.",
                    HttpStatus.CONFLICT);
        }

        if ("GOOGLE".equals(user.getProvider())
                && user.getProviderId() != null
                && !user.getProviderId().equals(providerId)) {
            throw new AuthException(
                    "Tài khoản này đã liên kết với một tài khoản Google khác.",
                    HttpStatus.CONFLICT);
        }

        user.setProvider("GOOGLE");
        user.setProviderId(providerId);
        user.setAvatarUrl(googleUserInfo.get("picture"));
        userRepository.save(user);
    }
}