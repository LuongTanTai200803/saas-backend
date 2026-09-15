package com.saasai.feature.payment;

import com.saasai.feature.payment.PaymentBankConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentBankConfigRepository extends JpaRepository<PaymentBankConfig, Long> {
    Optional<PaymentBankConfig> findFirstByIsActiveTrue();
}