package com.erp.products.repository;

import com.erp.products.domain.entity.Payment;
import com.erp.products.domain.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    void deleteBySaleId(Long saleId);

    List<Payment> findBySaleId(Long saleId);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            JOIN p.sale s
            WHERE (s.paymentSession.id = :sessionId
                   OR (s.paymentSession IS NULL AND s.posSession.id = :sessionId))
              AND s.status IN (com.erp.products.domain.enums.SaleStatus.PAID,
                               com.erp.products.domain.enums.SaleStatus.VALIDATED,
                               com.erp.products.domain.enums.SaleStatus.PARTIALLY_REFUNDED,
                               com.erp.products.domain.enums.SaleStatus.REFUNDED)
              AND p.method = :method
            """)
    BigDecimal sumBySessionAndMethod(@Param("sessionId") Long sessionId, @Param("method") PaymentMethod method);
}
