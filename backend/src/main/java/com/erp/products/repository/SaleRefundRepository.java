package com.erp.products.repository;

import com.erp.products.domain.entity.SaleRefund;
import com.erp.products.domain.enums.SaleRefundStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SaleRefundRepository extends JpaRepository<SaleRefund, Long> {

    long countByRefundNumberStartingWith(String prefix);

    Optional<SaleRefund> findByIdAndStatus(Long id, SaleRefundStatus status);

    List<SaleRefund> findBySaleIdOrderByCreatedAtDesc(Long saleId);

    @Query("""
            SELECT COALESCE(SUM(srl.quantity), 0)
            FROM SaleRefundLine srl
            JOIN srl.refund sr
            WHERE srl.saleLine.id = :saleLineId
              AND sr.status = :completed
            """)
    BigDecimal sumReturnedQuantityBySaleLine(@Param("saleLineId") Long saleLineId,
                                             @Param("completed") SaleRefundStatus completed);

    @Query("""
            SELECT COALESCE(SUM(sr.totalAmount), 0)
            FROM SaleRefund sr
            WHERE sr.sale.id = :saleId
              AND sr.status = :completed
            """)
    BigDecimal sumRefundedAmountBySale(@Param("saleId") Long saleId,
                                         @Param("completed") SaleRefundStatus completed);

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
              AND NOT EXISTS (SELECT 1 FROM RefundPayment rp WHERE rp.refund = sr)
            """)
    BigDecimal sumLegacyCashRefundsBySession(@Param("sessionId") Long sessionId,
                                             @Param("completed") SaleRefundStatus completed);

    @Query("""
            SELECT sr FROM SaleRefund sr
            JOIN sr.sale s
            WHERE sr.status = :completed
              AND (:saleNumber IS NULL OR LOWER(s.saleNumber) LIKE LOWER(CONCAT('%', :saleNumber, '%')))
            ORDER BY sr.createdAt DESC
            """)
    List<SaleRefund> searchCompleted(@Param("saleNumber") String saleNumber,
                                     @Param("completed") SaleRefundStatus completed,
                                     Pageable pageable);
}
