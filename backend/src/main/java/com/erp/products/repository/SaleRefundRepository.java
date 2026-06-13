package com.erp.products.repository;

import com.erp.products.domain.entity.SaleRefund;
import com.erp.products.domain.enums.SaleRefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface SaleRefundRepository extends JpaRepository<SaleRefund, Long> {

    long countByRefundNumberStartingWith(String prefix);

    @Query("""
            SELECT COALESCE(SUM(sr.totalAmount), 0)
            FROM SaleRefund sr
            JOIN sr.sale s
            WHERE sr.status = :completed
              AND (s.paymentSession.id = :sessionId
                   OR (s.paymentSession IS NULL AND s.posSession.id = :sessionId))
              AND EXISTS (
                  SELECT 1 FROM Payment p
                  WHERE p.sale = s AND p.method = com.erp.products.domain.enums.PaymentMethod.CASH
              )
            """)
    BigDecimal sumCashRefundsBySession(@Param("sessionId") Long sessionId,
                                       @Param("completed") SaleRefundStatus completed);
}
