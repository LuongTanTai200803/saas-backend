package com.saasai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.dto.AuthResponseDTO;
import com.saasai.dto.RefreshTokenRequestDTO;
import com.saasai.exception.AuthException;
import com.saasai.exception.GlobalExceptionHandler;
import com.saasai.security.TokenBlacklistService;
import com.saasai.service.AuthService;
import com.saasai.service.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthController authController;

    private AuthResponseDTO authResponseDTO;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerShouldReturnSuccess() throws Exception {
        doNothing().when(authService).registerUser(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "test@example.com",
                        "fullName", "Nguyen Van A",
                        "password", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo tài khoản thành công"))
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    void registerShouldReturnBadRequestWhenEmailExists() throws Exception {
        doThrow(new AuthException("Email đã tồn tại!", org.springframework.http.HttpStatus.BAD_REQUEST)).when(authService)
                .registerUser(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "test@example.com",
                        "fullName", "Nguyen Van A",
                        "password", "password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email đã tồn tại!"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void loginShouldReturnAccessAndRefreshTokensWhenCredentialsValid() throws Exception {
        AuthResponseDTO responseDTO = AuthResponseDTO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .role("ROLE_USER")
                .build();

        when(authService.loginUser(org.mockito.ArgumentMatchers.any()))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "test@example.com",
                        "password", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"));
    }

    @Test
    void refreshShouldReturnNewAccessTokenWhenRefreshTokenValid() throws Exception {
        when(authService.refreshAccessToken(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("new-access-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "refreshToken", "refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Làm mới Access Token thành công"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    void loginShouldReturnUnauthorizedWhenCredentialsInvalid() throws Exception {
        when(authService.loginUser(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new AuthException("Email hoặc mật khẩu không chính xác", org.springframework.http.HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "test@example.com",
                        "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không chính xác"))
                .andExpect(jsonPath("$.statusCode").value(401));
    }

    @Test
    void logoutShouldReturnBadRequestWhenAuthorizationHeaderInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authorization header không hợp lệ"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void logoutShouldReturnSuccessWhenTokenProvided() throws Exception {
        doNothing().when(tokenBlacklistService).blacklistToken(org.mockito.ArgumentMatchers.anyString());
        doNothing().when(refreshTokenService).revokeAllByEmail(org.mockito.ArgumentMatchers.anyString());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, java.util.List.of()) {{
                    setDetails("test@example.com");
                }});

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"))
                .andExpect(jsonPath("$.statusCode").value(200));
    }

        @Test
        void loginWithGoogle_Success() throws Exception {
        String validToken = "valid.google.id.token";

        when(authService.loginWithGoogle(validToken)).thenReturn(authResponseDTO);

        mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"" + validToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
        }

        @Test
        void loginWithGoogle_InvalidToken() throws Exception {
        when(authService.loginWithGoogle(anyString()))
                .thenThrow(new RuntimeException("Token Google không hợp lệ"));

        mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"invalid.token\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void loginWithGoogle_NewUser() throws Exception {
        // Test case tạo user mới từ Google
        String newToken = "new.google.token";
        when(authService.loginWithGoogle(newToken)).thenReturn(authResponseDTO);

        mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"" + newToken + "\"}"))
                .andExpect(status().isOk());
        }
}
