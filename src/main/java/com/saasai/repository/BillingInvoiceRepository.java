package com.saasai.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.saasai.entity.AdminPackageConfig;
import com.saasai.entity.BillingInvoice;

import java.util.Optional;

@Repository
public interface BillingInvoiceRepository extends JpaRepository<BillingInvoice, String> {
     public interface DailyRevenueProjection {
        String getLabel();
        Long getRevenue();
    }
    Optional<BillingInvoice> findByInvoiceIdAndUser_UserId(String invoiceId, String userId);

    Optional<BillingInvoice> findByMemoId(String memo);


    Optional<BillingInvoice> findById(String testInvoiceId);

    List<BillingInvoice> findByUser_UserIdOrderByCreatedAtDesc(String userId);
    Page<BillingInvoice> findByUser_UserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @Query("""
    SELECT COALESCE(SUM(i.finalAmount), 0)
    FROM BillingInvoice i
    WHERE i.status = com.saasai.entity.BillingInvoice.InvoiceStatus.PAID
      AND i.paymentDate >= :from
      AND i.paymentDate < :to
    """)
    long sumPaidAmountBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // Đếm số lượng hóa đơn đã thanh toán trong khoảng thời gian từ :from đến :to
    @Query("""
    SELECT COUNT(i)
    FROM BillingInvoice i
    WHERE i.status = com.saasai.entity.BillingInvoice.InvoiceStatus.PAID
      AND i.paymentDate >= :from
      AND i.paymentDate < :to
    """)
    long countPaidBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Lấy doanh thu hàng ngày trong khoảng thời gian từ :from đến :to
     */
    @Query(value = """
    SELECT
        DATE_FORMAT(i.payment_date, '%Y-%m-%d') AS label,
        COALESCE(SUM(i.final_amount), 0) AS revenue
    FROM billing_invoices i
    WHERE i.status = 'PAID'
      AND i.payment_date >= :from
      AND i.payment_date < :to
    GROUP BY DATE_FORMAT(i.payment_date, '%Y-%m-%d')
    ORDER BY DATE_FORMAT(i.payment_date, '%Y-%m-%d')
    """, nativeQuery = true)
List<DailyRevenueProjection> sumPaidAmountByDay(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
);

 
}
