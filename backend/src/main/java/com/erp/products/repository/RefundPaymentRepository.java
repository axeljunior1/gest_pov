package com.erp.products.repository;

import com.erp.products.domain.entity.RefundPayment;
import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.domain.enums.RefundPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface RefundPaymentRepository extends JpaRepository<RefundPayment, Long> {

    @Query("""
            SELECT COALESCE(SUM(rp.amount), 0)
            FROM RefundPayment rp
            WHERE rp.posSession.id = :sessionId
              AND rp.status = :status
              AND rp.method = :method
            """)
    BigDecimal sumBySessionAndMethod(@Param("sessionId") Long sessionId,
                                     @Param("method") PaymentMethod method,
                                     @Param("status") RefundPaymentStatus status);
}
