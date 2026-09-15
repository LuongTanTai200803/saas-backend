package com.saasai.feature.payment;

import com.saasai.feature.payment.PaymentBankConfigDTO;
import com.saasai.feature.payment.PaymentBankConfigRequest;
import com.saasai.feature.payment.PaymentBankConfig;
import com.saasai.feature.payment.PaymentBankConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentBankConfigService {

    @Autowired
    private PaymentBankConfigRepository repo;

    private PaymentBankConfigDTO toDto(PaymentBankConfig e) {
        if (e == null) return null;
        return new PaymentBankConfigDTO(
                e.getId(),
                e.getBankCode(),
                e.getAccountNumber(),
                e.getAccountName(),
                e.getVaNumber(),
                e.getTemplate(),
                e.getShowInfo(),
                e.getStore(),
                e.getIsActive(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public List<PaymentBankConfigDTO> listAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public PaymentBankConfigDTO getById(Long id) {
        return repo.findById(id).map(this::toDto).orElse(null);
    }

    @Transactional
    public PaymentBankConfigDTO create(PaymentBankConfigRequest req) {
        validateRequest(req);
        PaymentBankConfig e = PaymentBankConfig.builder()
                .bankCode(req.getBankCode())
                .accountNumber(req.getAccountNumber())
                .accountName(req.getAccountName())
                .vaNumber(req.getVaNumber())
                .template(req.getTemplate())
                .showInfo(req.getShowInfo() != null ? req.getShowInfo() : Boolean.TRUE)
                .store(req.getStore())
                .isActive(req.getIsActive() != null ? req.getIsActive() : Boolean.FALSE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PaymentBankConfig saved = repo.save(e);

        if (Boolean.TRUE.equals(saved.getIsActive())) {
            // ensure only one active: deactivate others
            deactivateOthers(saved.getId());
        }

        return toDto(saved);
    }

    @Transactional
    public PaymentBankConfigDTO update(Long id, PaymentBankConfigRequest req) {
        validateRequest(req);
        PaymentBankConfig e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Config not found: " + id));
        e.setBankCode(req.getBankCode());
        e.setAccountNumber(req.getAccountNumber());
        e.setAccountName(req.getAccountName());
        e.setVaNumber(req.getVaNumber());
        e.setTemplate(req.getTemplate());
        e.setShowInfo(req.getShowInfo() != null ? req.getShowInfo() : e.getShowInfo());
        e.setStore(req.getStore());
        e.setIsActive(req.getIsActive() != null ? req.getIsActive() : e.getIsActive());
        e.setUpdatedAt(LocalDateTime.now());

        PaymentBankConfig saved = repo.save(e);

        if (Boolean.TRUE.equals(saved.getIsActive())) {
            deactivateOthers(saved.getId());
        }

        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Transactional
    public PaymentBankConfigDTO activate(Long id) {
        PaymentBankConfig e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Config not found: " + id));
        e.setIsActive(Boolean.TRUE);
        e.setUpdatedAt(LocalDateTime.now());
        repo.save(e);
        deactivateOthers(id);
        return toDto(e);
    }

    private void deactivateOthers(Long activeId) {
        repo.findAll().stream()
                .filter(c -> !c.getId().equals(activeId) && Boolean.TRUE.equals(c.getIsActive()))
                .forEach(c -> {
                    c.setIsActive(Boolean.FALSE);
                    c.setUpdatedAt(LocalDateTime.now());
                    repo.save(c);
                });
    }

    private void validateRequest(PaymentBankConfigRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Request không được null"
            );
        }

        if (request.getBankCode() == null
                || request.getBankCode().isBlank()) {
            throw new IllegalArgumentException(
                    "bankCode không được để trống"
            );
        }

        if (request.getAccountNumber() == null
                || request.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "accountNumber không được để trống"
            );
        }

        if (request.getAccountName() == null
                || request.getAccountName().isBlank()) {
            throw new IllegalArgumentException(
                    "accountName không được để trống"
            );
        }
    }
}