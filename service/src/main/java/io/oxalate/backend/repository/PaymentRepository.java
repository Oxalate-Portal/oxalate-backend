package io.oxalate.backend.repository;

import io.oxalate.backend.model.Payment;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, Long> {
    @Query(nativeQuery = true,
            value = """
                    SELECT DISTINCT p.*
                    FROM payments p
                    WHERE p.user_id = :userId
                      AND p.payment_type = :paymentType
                      AND ((p.expires_at > NOW()
                            AND p.payment_type = 'PERIOD')
                           OR (p.payment_type = 'ONE_TIME'
                               AND p.payment_count > 0
                               AND (p.expires_at > NOW()
                                    OR p.expires_at IS NULL)))
                    """)
    Optional<Payment> findByUserIdAndAndPaymentType(@Param("userId") long userId, @Param("paymentType") String paymentType);

    List<Payment> findAllByUserId(long userId);

    @Query(nativeQuery = true, value = """
            SELECT * FROM payments
            WHERE user_id = :userId
              AND (expires_at > NOW()
                   OR expires_at IS NULL)
              AND (payment_type = 'PERIOD'
                   OR payment_type = 'ONE_TIME')
            """)
    Set<Payment> findAllActiveByUserId(@Param("userId") long userId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payments
            WHERE user_id = :userId
              AND ((expires_at > NOW()
                    AND payment_type = 'ONE_TIME')
                   OR (expires_at IS NULL
                       AND payment_type = 'ONE_TIME'))
            """)
    List<Payment> findActiveOneTimeByUserId(@Param("userId") long userId);

    @Query(nativeQuery = true,
            value = """
                    SELECT DISTINCT p.user_id
                    FROM payments p
                    WHERE (p.expires_at > NOW()
                           AND p.payment_type = 'PERIOD')
                       OR (p.payment_type = 'ONE_TIME'
                           AND p.payment_count > 0)
                    ORDER BY p.user_id
                    """)
    Set<Long> findAllUserIdWithActivePayments();

    @Query(nativeQuery = true, value = """
            UPDATE payments
            SET expires_at = NOW()
            WHERE (expires_at > NOW()
                   OR expires_at IS NULL)
              AND payment_type = :paymentType
            """)
    @Modifying
    void resetAllPayments(@Param("paymentType") String paymentType);

    @Query(nativeQuery = true,
            value = """
                    SELECT DISTINCT ON (p.user_id, p.payment_type) *
                    FROM payments p
                    WHERE (p.expires_at > NOW() OR p.expires_at IS NULL)
                      AND p.payment_type = :paymentType
                    ORDER BY p.user_id, p.payment_type, p.created_at DESC
                    """)
    List<Payment> findAllPaymentsWithActivePaymentsAndPaymentType(@Param("paymentType") String paymentType);
}
