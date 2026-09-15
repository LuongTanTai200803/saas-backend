package com.saasai.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import com.saasai.dto.ModelRoute;
import java.util.Optional;
import com.saasai.repository.UserRepository;
import com.saasai.repository.AdminPackageConfigRepository;
import com.saasai.repository.AiModelPackageRepository;
import com.saasai.entity.User;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.AiModelPackage;

import org.springframework.beans.factory.annotation.Value;



@Service
public class PackageRoutingService {

        @Value("${ai.max-tokens:2000}")
        private int configuredMaxTokens;
        @Value("${ai.temperature:0}")
        private double configuredTemperature;

        private static final int DEFAULT_MAX_TOKENS = Integer.parseInt(System.getProperty("AI_MAX_TOKENS", "2000"));
        private static final double DEFAULT_TEMPERATURE = Double.parseDouble(System.getProperty("AI_TEMPERATURE", "0"));

        
        private final ObjectMapper objectMapper;
        private final AiModelPackageRepository aiModelPackageRepository;
        

        public PackageRoutingService(ObjectMapper objectMapper, AiModelPackageRepository aiModelPackageRepository) {
                this.objectMapper = objectMapper;
                this.aiModelPackageRepository = aiModelPackageRepository;
        }

        // Service responsible for routing AI model requests based on the user's package configuration.
        @Transactional(readOnly = true)
        public ModelRoute resolveRoute(User user, String modelOption) {

        if (user == null) {
                throw new IllegalArgumentException("User required");
        }

        AdminPackageConfig pkg = user.getAdminPackageConfig();
        if (pkg == null) {
                throw new IllegalStateException("User has no package");
        }

        // User hiện tại tối thiểu level 1
        int userMaxLevel = pkg.getModelPackageLevel() != null
                ? pkg.getModelPackageLevel()
                : 1;

        // Mặc định level 1
        int requestedLevel = 1;

        // Client có gửi model option thì thử parse
        if (modelOption != null && !modelOption.isBlank()) {
                try {
                int level = Integer.parseInt(modelOption.trim());

                // Chỉ nhận level hợp lệ > 0
                if (level > 0) {
                        requestedLevel = level;
                }
                } catch (NumberFormatException ignored) {
                // Input sai -> mặc định level 1
                }
        }

        // Chỉ chặn trường hợp vượt quyền
        if (requestedLevel > userMaxLevel) {
                throw new IllegalArgumentException(
                        "Requested model package level not allowed for this user"
                );
        }

        // Load model package
        AiModelPackage modelPackage = aiModelPackageRepository
                .findById((long) requestedLevel)
                .orElseThrow(() ->
                        new IllegalStateException("Model package not found")
                );

        if (!Boolean.TRUE.equals(modelPackage.getActive())) {
                throw new IllegalStateException("Model package is not active");
        }

        try {
                List<String> models = objectMapper.readValue(
                        modelPackage.getModels(),
                        new TypeReference<List<String>>() {}
                );

                if (models == null || models.isEmpty()) {
                throw new IllegalStateException("Model package contains no models");
                }

                String primary = validateModelId(models.get(0));

                String fallback = models.size() > 1
                        ? validateModelId(models.get(1))
                        : null;

                return new ModelRoute(
                        primary,
                        fallback,
                        configuredMaxTokens > 0
                                ? configuredMaxTokens
                                : DEFAULT_MAX_TOKENS,
                        configuredTemperature >= 0
                                ? configuredTemperature
                                : DEFAULT_TEMPERATURE,
                        modelPackage.getId(),
                        modelPackage.getCreditRate()
                );

        } catch (Exception ex) {
                throw new IllegalStateException(
                        "Malformed models in model package",
                        ex
                );
        }
        }

        // Phương thức parseAllowedModels và normalizeModelId được sử dụng để xử lý danh sách model được phép từ cấu hình gói dịch vụ.
        private List<String> parseAllowedModels(String json) {
                if (json == null || json.isBlank()) {
                return List.of();
                }

                try {
                return objectMapper.readValue(
                        json,
                        new TypeReference<List<String>>() {}
                );
                } catch (JsonProcessingException exception) {
                throw new IllegalStateException(
                        "allowed_models không đúng định dạng JSON",
                        exception
                );
                }
        }

        private String validateModelId(String model) {
                if (model == null || model.isBlank()) {
                        throw new IllegalStateException(
                                "Model trong allowed_models không hợp lệ"
                        );
                }

                return model.trim();
                }
        }