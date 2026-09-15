package com.saasai;

import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class AssistAIApplication {
    private static final Logger logger = LoggerFactory.getLogger(AssistAIApplication.class);
    
    @PostConstruct
    public void initTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        logger.info("Default timezone={}", TimeZone.getDefault().getID());
    }

    public static void main(String[] args) {
        SpringApplication.run(AssistAIApplication.class, args);
    }
}
