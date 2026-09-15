package com.saasai.ratelimit;

import com.saasai.config.RateLimitProperties;
import com.saasai.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor // 🎯 Bơm biến final tự động qua Constructor sạch sẽ
public class RateLimitService {

    private final RateLimitProperties rateLimitProperties; // 🎯 Thay thế cho một mớ @Value cũ
    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    public void validateLogin(String key) {
        validate(key, rateLimitProperties.getAuth().getLoginLimit(), "Too many login attempts");
    }

    public void validateCreditEstimate(String key) {
        validate(key, rateLimitProperties.getCredits().getEstimateLimit(), "Too many credit estimate requests");
    }

    public void validateFileUpload(String key) {
        validate(key, rateLimitProperties.getFiles().getUploadLimit(), "Too many file upload requests");
    }

    public void validateAiCompletion(String key) {
        validate(key, rateLimitProperties.getAi().getCompletionsLimit(), "Too many AI completion requests");
    }

    private void validate(String key, int limit, String message) {
        // Đọc cửa sổ thời gian trực tiếp từ file cấu hình gác cổng
        RateBucket bucket = buckets.computeIfAbsent(key, k -> new RateBucket(limit, rateLimitProperties.getWindowSeconds()));
        if (!bucket.tryConsume()) {
            throw new TooManyRequestsException(message + " - please try again later");
        }
    }

    // Giữ nguyên logic xử lý luồng đồng bộ của RateBucket ở dưới...
    private static class RateBucket {
        private final int limit;
        private final int windowSeconds;
        private Instant windowStart;
        private final AtomicInteger count;

        public RateBucket(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
            this.windowStart = Instant.now();
            this.count = new AtomicInteger(0);
        }

        public synchronized boolean tryConsume() {
            Instant now = Instant.now();
            if (Duration.between(windowStart, now).getSeconds() >= windowSeconds) {
                windowStart = now;
                count.set(0);
            }
            if (count.incrementAndGet() > limit) {
                return false;
            }
            return true;
        }
    }

    public boolean tryConsume(String string, int i, int j) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tryConsume'");
    }
}