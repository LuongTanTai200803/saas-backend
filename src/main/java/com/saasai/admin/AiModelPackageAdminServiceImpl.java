package com.saasai.admin;

import com.saasai.entity.AiModelPackage;
import com.saasai.repository.AiModelPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class AiModelPackageAdminServiceImpl
        implements AiModelPackageAdminService {

    private final AiModelPackageRepository repository;

    private final ObjectMapper objectMapper;

    public AiModelPackageAdminServiceImpl(
            AiModelPackageRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModelPackageDTO> listModelPackages() {
        return repository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AiModelPackageDTO updateCreditRate(
            Long id,
            CreditRateUpdateRequest request
    ) {
        if (request == null
                || request.getCreditRate() == null
                || request.getCreditRate() <= 0) {
            throw new IllegalArgumentException(
                    "creditRate phải lớn hơn 0"
            );
        }

        if (request.getCreditRate() > 100) {
            throw new IllegalArgumentException(
                    "creditRate không được lớn hơn 100"
            );
        }

        AiModelPackage modelPackage = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Model package không tồn tại: " + id
                ));

        modelPackage.setCreditRate(request.getCreditRate());

        return toDto(repository.save(modelPackage));
    }

    private AiModelPackageDTO toDto(AiModelPackage modelPackage) {
        return new AiModelPackageDTO(
                modelPackage.getId(),
                modelPackage.getCode(),
                modelPackage.getName(),
                modelPackage.getCreditRate(),
                modelPackage.getModels(),
                modelPackage.getDescription(),
                modelPackage.getActive()
        );
    }

    @Override
        @Transactional
        public AiModelPackageDTO update(
                Long id,
                AiModelPackageUpdateRequest request
        ) {
        if (request == null) {
                throw new IllegalArgumentException(
                        "Request không được null"
                );
        }
        AiModelPackage modelPackage = repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Model package không tồn tại: " + id
                        ));

        if (request.getCode() != null
                && !request.getCode().isBlank()) {
                modelPackage.setCode(request.getCode().trim());
        }

        if (request.getName() != null
                && !request.getName().isBlank()) {
                modelPackage.setName(request.getName().trim());
        }

        if (request.getCreditRate() != null) {
                if (request.getCreditRate() <= 0
                        || request.getCreditRate() > 100) {
                throw new IllegalArgumentException(
                        "creditRate phải lớn hơn 0 và không được vượt quá 100"
                );
                }

                modelPackage.setCreditRate(request.getCreditRate());
        }

        if (request.getModels() != null) {
                modelPackage.setModels(
                        validateModelsJson(request.getModels())
                );
        }

        if (request.getDescription() != null) {
                modelPackage.setDescription(request.getDescription());
        }

        if (request.getActive() != null) {
                modelPackage.setActive(request.getActive());
        }

        return toDto(repository.save(modelPackage));
        }

        private String validateModelsJson(String modelsJson) {
        if (modelsJson == null || modelsJson.isBlank()) {
                throw new IllegalArgumentException(
                        "models không được để trống"
                );
        }

        try {
                List<String> models = objectMapper.readValue(
                        modelsJson,
                        new TypeReference<List<String>>() {}
                );

                if (models.isEmpty()) {
                throw new IllegalArgumentException(
                        "models phải chứa ít nhất một model"
                );
                }

                for (String model : models) {
                if (model == null || model.isBlank()) {
                        throw new IllegalArgumentException(
                                "models không được chứa model rỗng"
                        );
                }
                }

                List<String> normalizedModels = models.stream()
                        .map(String::trim)
                        .distinct()
                        .toList();

                return objectMapper.writeValueAsString(normalizedModels);

        } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException(
                        "models phải là JSON array hợp lệ, ví dụ "
                                + "[\"gpt-4o-mini\",\"claude-3-haiku\"]",
                        exception
                );
        }
}

        private String serializeModels(List<String> models) {
        if (models == null || models.isEmpty()) {
                throw new IllegalArgumentException(
                        "models phải chứa ít nhất một model"
                );
        }

        List<String> normalizedModels = models.stream()
                .map(model -> {
                        if (model == null || model.isBlank()) {
                        throw new IllegalArgumentException(
                                "models không được chứa model rỗng"
                        );
                        }
                        return model.trim();
                })
                .distinct()
                .toList();

        try {
                return objectMapper.writeValueAsString(normalizedModels);
        } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException(
                        "Không thể serialize models",
                        exception
                );
        }
        }
}