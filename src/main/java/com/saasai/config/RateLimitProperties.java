package com.saasai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "ratelimit") // 🎯 Bốc toàn bộ các key bắt đầu bằng "ratelimit"
@Getter
@Setter
public class RateLimitProperties {
    
    private Auth auth = new Auth();
    private Credits credits = new Credits();
    private Files files = new Files();
    private Ai ai = new Ai();
    private int windowSeconds = 60; // Giá trị mặc định nếu file properties thiếu

    @Getter
    @Setter
    public static class Auth {
        private int loginLimit = 10;
    }

    @Getter
    @Setter
    public static class Credits {
        private int estimateLimit = 30;
    }

    @Getter
    @Setter
    public static class Files {
        private int uploadLimit = 10;
    }

    @Getter
    @Setter
    public static class Ai {
        private int completionsLimit = 20;
    }
}