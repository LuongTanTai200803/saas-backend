package com.saasai.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.dto.PackageDTO;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.repository.AdminPackageConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/packages")
public class PackagesController {

    @Autowired
    private AdminPackageConfigRepository adminPackageConfigRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Locale VN = new Locale("vi", "VN");
    private final NumberFormat numberFormat = NumberFormat.getInstance(VN);

    @GetMapping
    public ResponseEntity<List<PackageDTO>> listPackages() {
        List<AdminPackageConfig> configs = adminPackageConfigRepository.findAll();

        List<PackageDTO> dtos = configs.stream()
                .map(this::toDto)
                .sorted(Comparator
                        .comparing(PackageDTO::isFree, Comparator.nullsLast(Comparator.reverseOrder())) // free first
                        .thenComparing(p -> Optional.ofNullable(p.price()).orElse(Long.MAX_VALUE))
                        .thenComparing(PackageDTO::packageType))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{packageType}")
    public ResponseEntity<PackageDTO> getPackage(@PathVariable String packageType) {
        AdminPackageConfig cfg = adminPackageConfigRepository
                .findByPackageType(packageType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found: " + packageType));

        return ResponseEntity.ok(toDto(cfg));
    }

    private PackageDTO toDto(AdminPackageConfig cfg) {

        Long price = cfg.getPrice();

        boolean isFree = price == null || price == 0L;

        String displayPrice;

        if (isFree) {
            displayPrice = "Miễn phí";
        } else {
            displayPrice = numberFormat.format(price) + " ₫";

            if (cfg.getPackageCategory()
                    == AdminPackageConfig.PackageCategory.SUBSCRIPTION) {
                displayPrice += "/tháng";
            }
        }

        String durationHuman = switch (cfg.getPackageCategory()) {
            case SUBSCRIPTION -> cfg.getDuration() + " ngày";
            case CREDIT_PACK -> cfg.getDuration() + " ngày";
        };

        String badge = null;
        Boolean canPurchase = true;

        return new PackageDTO(
                    cfg.getId(),
                    cfg.getPackageType(),
                    cfg.getPackageCategory() != null ? cfg.getPackageCategory().name() : null,
                    price,
                    displayPrice,
                    cfg.getCreditLimit(),
                    cfg.getDuration(),
                    durationHuman,
                    cfg.getDescription(),
                    cfg.getStorageQuotaMb(),
                    isFree,
                    canPurchase,
                    cfg.getCreatedAt(),
                    cfg.getUpdatedAt(),
                    badge
        );
    }

}