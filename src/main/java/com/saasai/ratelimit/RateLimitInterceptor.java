package com.saasai.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String key = getKeyForRequest(request);

        // Rate limit cho Google Login
        if (uri.equals("/api/v1/auth/google")) {
            if (!rateLimitService.tryConsume(key + ":google", 5, 60)) { // 5 lần / 60 giây
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many Google login attempts. Please try again later.\"}");
                return false;
            }
        } 
        // Các endpoint cũ
        else if (uri.equals("/api/v1/auth/login")) {
            rateLimitService.validateLogin(key);
        } else if (uri.equals("/api/v1/credits/estimate")) {
            rateLimitService.validateCreditEstimate(key);
        } else if (uri.equals("/api/v1/files/upload")) {
            rateLimitService.validateFileUpload(key);
        } else if (uri.equals("/api/v1/ai/completions")) {
            rateLimitService.validateAiCompletion(key);
        }

        return true;
    }

    private String getKeyForRequest(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return request.getRequestURI() + ":user:" + userId;
        }
        return request.getRequestURI() + ":ip:" + resolveClientIp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}