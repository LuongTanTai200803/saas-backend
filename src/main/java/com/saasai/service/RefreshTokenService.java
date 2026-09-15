package com.saasai.service;

import com.saasai.entity.RefreshToken;
import com.saasai.entity.User;
import com.saasai.exception.AuthException;
import com.saasai.repository.RefreshTokenRepository;
import com.saasai.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserRepository userRepository;

    public String createRefreshToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Người dùng không tồn tại", HttpStatus.UNAUTHORIZED));

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                // .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthException("Refresh token không hợp lệ hoặc đã bị đăng xuất", HttpStatus.UNAUTHORIZED));

        // Nếu bản ghi tồn tại dưới DB tức là chưa bị xóa (chưa revoke), chỉ cần check hết hạn
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AuthException("Refresh token đã hết hạn", HttpStatus.UNAUTHORIZED);
        }
        return refreshToken;
    }

    @Transactional
    public void revokeAllByEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        refreshTokenRepository.deleteByUserEmail(email);
    }
}
