package com.saasai.feature.payment;

import com.saasai.entity.User;
import com.saasai.feature.payment.CreditAccount;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional; 
public interface CreditAccountRepository extends JpaRepository<CreditAccount, String> { 
    
    List<CreditAccount> findByPurchasedCreditExpireAtLessThanEqual(
        LocalDateTime now
    );

    // Dùng @Modifying và @Query để thực hiện cập nhật trực tiếp trong cơ sở dữ liệu
    @Modifying
    @Query("""
        UPDATE CreditAccount c
        SET c.purchasedCreditBalance = 0,
            c.purchasedCreditPurchasedAt = null,
            c.purchasedCreditExpireAt = null
        WHERE c.purchasedCreditExpireAt <= :now
    """)
    int expirePurchasedCredits(@Param("now") LocalDateTime now);

    List<CreditAccount> findByUser(User user);
}
