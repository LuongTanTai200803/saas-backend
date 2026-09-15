package com.saasai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceTest {
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void blacklistToken_shouldReportBlacklistedUntilExpiration() {
        String token = "test-token";
        Date future = new Date(System.currentTimeMillis() + 10000);
        when(jwtTokenProvider.getExpirationFromJWT(token)).thenReturn(future);

        tokenBlacklistService.blacklistToken(token);
        assertThat(tokenBlacklistService.isBlacklisted(token)).isTrue();
    }
}
