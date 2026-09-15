package com.saasai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTR = "AUTH_ERROR";
    public static final String AUTH_ERROR_TOKEN_MISSING = "TOKEN_MISSING";
    public static final String AUTH_ERROR_TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String AUTH_ERROR_TOKEN_INVALID = "TOKEN_INVALID";
    public static final String AUTH_ERROR_TOKEN_BLACKLISTED = "TOKEN_BLACKLISTED";

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        
        // Danh sách các path không cần JWT
        return path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/google")           // Google Login
                || path.startsWith("/oauth2/authorization")     // Bắt đầu flow Google
                || path.startsWith("/login/oauth2/code")        // Callback từ Google
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/h2-console")
                || path.equals("/api/v1/sepay/webhook");        // SEPAY webhook không cần JWT
    }

    @Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
    try {
        String jwt = getJwtFromRequest(request);

        JwtTokenProvider.TokenError tokenError = tokenProvider.getTokenError(jwt);
        if (tokenError == null) {
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_TOKEN_BLACKLISTED);
            } else {
                String userId = tokenProvider.getUserIdFromJWT(jwt);
                String email = tokenProvider.getEmailFromJWT(jwt);
                String role = tokenProvider.getRoleFromJWT(jwt);
                if (role == null) {
                    role = "ROLE_USER";
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.singletonList(new SimpleGrantedAuthority(role)));
                authentication.setDetails(email);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } else if (tokenError == JwtTokenProvider.TokenError.MISSING) {
            request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_TOKEN_MISSING);
        } else if (tokenError == JwtTokenProvider.TokenError.EXPIRED) {
            request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_TOKEN_EXPIRED);
        } else {
            request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_TOKEN_INVALID);
        }
    } catch (Exception ex) {
        logger.error("Could not set user authentication in security context", ex);
        request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_TOKEN_INVALID);
    }

    filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
