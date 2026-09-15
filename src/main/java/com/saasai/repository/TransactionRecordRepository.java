package com.saasai.repository;

import com.saasai.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    Optional<TransactionRecord> findByExternalTransactionId(String externalTransactionId);
    Optional<TransactionRecord> findByInvoiceId(String invoiceId);
    Optional<TransactionRecord> findByMemoId(String memoId);

    /**
     * Sums the final amounts of paid billing invoices between the specified date range.
     *
     * @param from the start of the date range (inclusive)
     * @param to   the end of the date range (exclusive)
     * @return the total sum of paid amounts within the specified date range
     */
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

    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String status,
            LocalDateTime from,
            LocalDateTime to
    );
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime startOfMonth,
            LocalDateTime startOfTomorrow);

        @Query("""
        SELECT t
        FROM TransactionRecord t
        JOIN FETCH t.user u
        WHERE (:status IS NULL OR t.status = :status)
        AND (:userId IS NULL OR u.userId = :userId)
        AND (:from IS NULL OR t.createdAt >= :from)
        AND (:to IS NULL OR t.createdAt < :to)
        ORDER BY t.createdAt DESC
        """)
        List<TransactionRecord> search(
                @Param("status") String status,
                @Param("userId") String userId,
                @Param("from") LocalDateTime from,
                @Param("to") LocalDateTime to
        );
}