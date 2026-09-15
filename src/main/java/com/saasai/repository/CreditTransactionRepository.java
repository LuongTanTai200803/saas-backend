package com.saasai.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.saasai.entity.CreditTransaction;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, String> {
        public interface DailyTokenProjection {
                String getLabel();
                Long getTotalTokens();
        }
        public interface TopAssistantProjection {
                Integer getAssistantId();
                String getName();
                Long getTotalTokens();
                Double getCreditsConsumed();
        }

    long countByUser_UserIdAndType(String userId, CreditTransaction.TransactionType type);
    long countByUser_UserIdAndTypeAndDescriptionContainingIgnoreCase(String userId, CreditTransaction.TransactionType type, String keyword);

    /**
     * Counts the number of credit transactions of a specific type within the specified date range.
     *
     * @param type the type of credit transaction
     * @param from the start of the date range (inclusive)
     * @param to   the end of the date range (exclusive)
     * @return the total count of credit transactions of the specified type within the date range
     */
    @Query("""
    SELECT COUNT(ct)
    FROM CreditTransaction ct
    WHERE ct.type = :type
      AND ct.createdAt >= :from
      AND ct.createdAt < :to
    """)
    long countByTypeBetween(
            @Param("type") CreditTransaction.TransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Sums the actual credit deducted for credit transactions of a specific type within the specified date range.
     *
     * @param type the type of credit transaction
     * @param from the start of the date range (inclusive)
     * @param to   the end of the date range (exclusive)
     * @return the total sum of actual credit deducted within the specified date range
     */
    @Query("""
        SELECT COALESCE(SUM(ct.actualCreditDeducted), 0)
        FROM CreditTransaction ct
        WHERE ct.type = :type
        AND ct.createdAt >= :from
        AND ct.createdAt < :to
        """)
    double sumActualCreditByTypeBetween(
            @Param("type") CreditTransaction.TransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    List<CreditTransaction>
        findByUser_UserIdOrderByCreatedAtDesc(String userId);


        /**
         * Sums the prompt tokens for credit transactions of a specific type within the specified date range.
         *
         * @param type the type of credit transaction
         * @param from the start of the date range (inclusive)
         * @param to   the end of the date range (exclusive)
         * @return the total sum of prompt tokens within the specified date range
         */
        @Query("""
    SELECT COALESCE(SUM(ct.promptTokens), 0)
    FROM CreditTransaction ct
    WHERE ct.type = :type
      AND ct.createdAt >= :from
      AND ct.createdAt < :to
    """)
        long sumPromptTokensByTypeBetween(
                @Param("type") CreditTransaction.TransactionType type,
                @Param("from") LocalDateTime from,
                @Param("to") LocalDateTime to
        );

        /**
         * Sums the completion tokens for credit transactions of a specific type within the specified date range.
         *
         * @param type the type of credit transaction
         * @param from the start of the date range (inclusive)
         * @param to   the end of the date range (exclusive)
         * @return the total sum of completion tokens within the specified date range
         */
        @Query("""
    SELECT COALESCE(SUM(ct.completionTokens), 0)
    FROM CreditTransaction ct
    WHERE ct.type = :type
      AND ct.createdAt >= :from
      AND ct.createdAt < :to
    """)
        long sumCompletionTokensByTypeBetween(
                @Param("type") CreditTransaction.TransactionType type,
                @Param("from") LocalDateTime from,
                @Param("to") LocalDateTime to
        );

        /**
         * Sums the total tokens for credit transactions of a specific type within the specified date range.
         *
         * @param type the type of credit transaction
         * @param from the start of the date range (inclusive)
         * @param to   the end of the date range (exclusive)
         * @return the total sum of total tokens within the specified date range
         */
        @Query("""
    SELECT COALESCE(SUM(ct.totalTokens), 0)
    FROM CreditTransaction ct
    WHERE ct.type = :type
      AND ct.createdAt >= :from
      AND ct.createdAt < :to
    """)
        long sumTotalTokensByTypeBetween(
                @Param("type") CreditTransaction.TransactionType type,
                @Param("from") LocalDateTime from,
                @Param("to") LocalDateTime to
        );

        /**
         * Retrieves the total tokens for credit transactions of type 'DEDUCT' grouped by day within the specified date range.
         *
         * @param from the start of the date range (inclusive)
         * @param to   the end of the date range (exclusive)
         * @return a list of daily total tokens projections
         */
       @Query(value = """
    SELECT
        DATE_FORMAT(ct.created_at, '%Y-%m-%d') AS label,
        COALESCE(SUM(ct.total_tokens), 0) AS totalTokens
    FROM credit_transactions ct
    WHERE ct.type = 'DEDUCT'
      AND ct.created_at >= :from
      AND ct.created_at < :to
    GROUP BY DATE_FORMAT(ct.created_at, '%Y-%m-%d')
    ORDER BY DATE_FORMAT(ct.created_at, '%Y-%m-%d')
    """, nativeQuery = true)
List<DailyTokenProjection> sumTotalTokensByDay(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
);

        /**
         * Retrieves the top assistants based on total tokens consumed within the specified date range.
         *
         * @param from the start of the date range (inclusive)
         * @param to   the end of the date range (exclusive)
         * @param pageable the pagination information
         * @return a list of top assistant projections
         */
        @Query(value = """
    SELECT
        a.assistant_id AS assistantId,
        a.assistant_name AS name,
        COALESCE(SUM(ct.total_tokens), 0) AS totalTokens,
        COALESCE(SUM(ct.actual_credit_deducted), 0) AS creditsConsumed
    FROM credit_transactions ct
    INNER JOIN chat_sessions cs
        ON cs.session_id = ct.session_id
    INNER JOIN assistants a
        ON a.assistant_id = cs.assistant_id
    WHERE ct.type = 'DEDUCT'
      AND ct.created_at >= :from
      AND ct.created_at < :to
    GROUP BY a.assistant_id, a.assistant_name
    ORDER BY totalTokens DESC
    """, nativeQuery = true)
        List<TopAssistantProjection> findTopAssistants(
                @Param("from") LocalDateTime from,
                @Param("to") LocalDateTime to,
                Pageable pageable
        );
}
