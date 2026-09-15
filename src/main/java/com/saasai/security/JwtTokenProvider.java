package com.saasai.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.access-expiration:90000000}") // Default to 25 hours if not set
    private long jwtAccessExpirationMs;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private final GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        System.out.println("=== Google Client ID: " + googleClientId);
        if (googleClientId == null || googleClientId.isEmpty()) {
            System.err.println("WARNING: Google Client ID is empty!");
        }
    }

    public JwtTokenProvider(GoogleIdTokenVerifier verifier) {
        this.verifier = new GoogleIdTokenVerifier.Builder(verifier.getTransport(), verifier.getJsonFactory())
                .setAudience(java.util.Collections.singletonList(googleClientId))
                .build();
    }

    @PostConstruct
    public void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT secret must be provided and at least 32 characters long");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateAccessToken(String userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtAccessExpirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
        return claims.get("userId", String.class);
    }

    public String getEmailFromJWT(String token) {
        Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    public String getRoleFromJWT(String token) {
        Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
        return claims.get("role", String.class);
    }

    public Date getExpirationFromJWT(String token) {
        Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
        return claims.getExpiration();
    }

    public enum TokenError {
    MISSING,
    EXPIRED,
    INVALID
    }

        public TokenError getTokenError(String token) {
            if (token == null || token.isBlank()) {
                return TokenError.MISSING;
            }
            try {
                Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
                return null;
            } catch (ExpiredJwtException ex) {
                return TokenError.EXPIRED;
            } catch (JwtException | IllegalArgumentException ex) {
                return TokenError.INVALID;
            }
        }

        public boolean validateToken(String authToken) {
            return getTokenError(authToken) == null;
        }

        public Map<String, String> verifyGoogleIdToken(String googleIdToken) {
            try {
                if (googleIdToken == null || googleIdToken.trim().isEmpty()) {
                    throw new RuntimeException("Google ID Token không được để trống");
                }

                GoogleIdToken idToken = verifier.verify(googleIdToken);

                if (idToken == null) {
                    throw new RuntimeException("Token Google không hợp lệ");
                }

                GoogleIdToken.Payload payload = idToken.getPayload();

                Map<String, String> userInfo = new HashMap<>();
                userInfo.put("email", payload.getEmail());
                userInfo.put("name", (String) payload.get("name"));
                userInfo.put("picture", (String) payload.get("picture"));
                userInfo.put("sub", payload.getSubject()); // Google user ID

                System.out.println("[TokenProvider] Verify Google token thành công cho email: " + payload.getEmail());

                return userInfo;

            } catch (Exception e) {
                System.err.println("[TokenProvider] Lỗi verify Google token: " + e.getMessage());
                if (e.getMessage().contains("expired") || e.getMessage().contains("Invalid")) {
                    throw new RuntimeException("Token Google đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.");
                }
                throw new RuntimeException("Xác thực Google thất bại: " + e.getMessage());
            }
        }

}