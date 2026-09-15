package com.saasai.controller;

import com.saasai.feature.ai.ApiResponseDTO;
import com.saasai.dto.AuthResponseDTO;
import com.saasai.dto.LoginRequestDTO;
import com.saasai.dto.RefreshTokenRequestDTO;
import com.saasai.dto.RegisterRequestDTO;
import com.saasai.exception.AuthException;
import com.saasai.security.TokenBlacklistService;
import com.saasai.service.AuthService;
import com.saasai.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<Object>> register(@Valid @RequestBody RegisterRequestDTO request) {
        authService.registerUser(request);
        logger.info("User registered successfully: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponseDTO.builder()
                .success(true)
                .message("Tạo tài khoản thành công")
                .statusCode(200)
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        logger.info("LOGIN REQUEST email={}", request.getEmail());
        AuthResponseDTO loginResponse = authService.loginUser(request);
        
        logger.info("User logged in successfully: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponseDTO.<AuthResponseDTO>builder()
                .success(true)
                .message("Đăng nhập thành công")
                .statusCode(200)
                .data(loginResponse)
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        String accessToken = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponseDTO.<Map<String, String>>builder()
                .success(true)
                .message("Làm mới Access Token thành công")
                .statusCode(200)
                .data(Map.of("accessToken", accessToken))
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<Object>> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            logger.warn("Invalid Authorization header received for logout request");
            throw new AuthException("Authorization header không hợp lệ", HttpStatus.BAD_REQUEST);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof String)) {
            throw new AuthException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        String currentEmail = (String) authentication.getDetails();
        String token = bearerToken.substring(7);
        try {
            tokenBlacklistService.blacklistToken(token);
        } catch (Exception ex) {
            logger.warn("Could not blacklist access token during logout: {}", ex.getMessage());
        }

        refreshTokenService.revokeAllByEmail(currentEmail);
        logger.info("User logged out successfully: {}", currentEmail);
        return ResponseEntity.ok(ApiResponseDTO.builder()
                .success(true)
                .message("Đăng xuất thành công")
                .statusCode(200)
                .build());
    }

    public AuthController(AuthService authService, TokenBlacklistService tokenBlacklistService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> loginWithGoogle(@RequestParam("token") String googleToken) {
        AuthResponseDTO authResponse = authService.loginWithGoogle(googleToken);
        return ResponseEntity.ok(ApiResponseDTO.<AuthResponseDTO>builder()
                .success(true)
                .message("Đăng nhập bằng Google thành công")
                .statusCode(200)
                .data(authResponse)
                .build());

    }
}
