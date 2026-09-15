package com.saasai.security;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public void blacklistToken(String token) {
        Date expiration = jwtTokenProvider.getExpirationFromJWT(token);
        if (expiration != null) {
            blacklist.put(token, expiration);
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        Date expiration = blacklist.get(token);
        if (expiration == null) {
            return false;
        }
        if (expiration.before(new Date())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
