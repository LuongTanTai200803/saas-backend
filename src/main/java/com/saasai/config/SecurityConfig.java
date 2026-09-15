package com.saasai.config;

import org.springframework.beans.factory.annotation.Value;

import com.saasai.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html");
    }

    // 🎯 BỔ SUNG: Cấu hình CORS chi tiết cho môi trường Production (Vercel) và Local
    @Value("${frontend.allowed-origins:http://localhost:5173}")
    private String frontendAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // 1. Tách chuỗi theo dấu phẩy và xóa sạch khoảng trắng thừa
        List<String> origins = Arrays.stream(frontendAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        // 2. Dùng setAllowedOriginPatterns để tránh xung đột với allowCredentials
        configuration.setAllowedOriginPatterns(origins);

        // 3. Các method cho phép (bắt buộc phải có OPTIONS cho preflight)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 4. Cho phép mọi header và cookie/auth
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight 1 tiếng để giảm số lần gọi OPTIONS

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                                Object authErrorObj = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTR);
                                String authError = authErrorObj != null ? authErrorObj.toString() : "";

                                String message;
                                if (JwtAuthenticationFilter.AUTH_ERROR_TOKEN_EXPIRED.equals(authError)) {
                                    message = "Access token đã hết hạn, vui lòng đăng nhập lại.";
                                } else if (JwtAuthenticationFilter.AUTH_ERROR_TOKEN_BLACKLISTED.equals(authError)) {
                                    message = "Phiên đăng nhập đã bị thu hồi, vui lòng đăng nhập lại.";
                                } else if (JwtAuthenticationFilter.AUTH_ERROR_TOKEN_INVALID.equals(authError)) {
                                    message = "Access token không hợp lệ.";
                                } else {
                                    message = "Bạn chưa đăng nhập. Vui lòng đăng nhập lại.";
                                }

                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write(
                                        "{\"success\":false,\"message\":\"" + message + "\",\"statusCode\":401}"
                                );
                            }))
                .authorizeHttpRequests(auth -> auth
                        // 🚀 CHÍ MẠNG: Cho phép tất cả các request OPTIONS (Preflight) đi qua không cần kiểm tra Token
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/google").permitAll()
                        .requestMatchers(
                            "/api-docs",
                            "/api-docs/**",
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/v1/sepay/webhook").permitAll()
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC).permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}