package com.erp.products.repository;

import com.erp.products.domain.entity.SaleRefund;
import com.erp.products.domain.enums.SaleRefundStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
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

    @Query("""
            SELECT sr.sale.id, COUNT(sr), COALESCE(SUM(CASE WHEN sr.status = :completed THEN sr.totalAmount ELSE 0 END), 0)
            FROM SaleRefund sr
            WHERE sr.sale.id IN :saleIds
            GROUP BY sr.sale.id
            """)
    List<Object[]> aggregateBySaleIds(@Param("saleIds") Collection<Long> saleIds,
                                      @Param("completed") SaleRefundStatus completed);

    @Query("""
            SELECT rl.refund.id, COUNT(rl)
            FROM SaleRefundLine rl
            WHERE rl.refund.id IN :refundIds
            GROUP BY rl.refund.id
            """)
    List<Object[]> countLinesByRefundIds(@Param("refundIds") Collection<Long> refundIds);

    @EntityGraph(attributePaths = {"sale", "sale.customer", "cashier"})
    @Query("""
            SELECT sr FROM SaleRefund sr
            JOIN sr.sale s
            LEFT JOIN s.customer c
            WHERE (:filterStatus = false OR sr.status = :status)
            AND (:filterSaleId = false OR s.id = :saleId)
            AND (:filterQ = false
                 OR LOWER(sr.refundNumber) LIKE CONCAT('%', :q, '%')
                 OR LOWER(s.saleNumber) LIKE CONCAT('%', :q, '%')
                 OR LOWER(c.firstName) LIKE CONCAT('%', :q, '%')
                 OR LOWER(c.lastName) LIKE CONCAT('%', :q, '%')
                 OR LOWER(c.customerNumber) LIKE CONCAT('%', :q, '%'))
            AND (:filterDateFrom = false OR sr.createdAt >= :dateFrom)
            AND (:filterDateTo = false OR sr.createdAt <= :dateTo)
            AND (:filterUserId = false
                 OR sr.cashier.id = :userId
                 OR s.seller.id = :userId
                 OR s.cashier.id = :userId)
            ORDER BY sr.createdAt DESC, sr.id DESC
            """)
    List<SaleRefund> searchBrowseReturns(
            @Param("filterStatus") boolean filterStatus,
            @Param("status") SaleRefundStatus status,
            @Param("filterSaleId") boolean filterSaleId,
            @Param("saleId") Long saleId,
            @Param("filterQ") boolean filterQ,
            @Param("q") String q,
            @Param("filterDateFrom") boolean filterDateFrom,
            @Param("dateFrom") Instant dateFrom,
            @Param("filterDateTo") boolean filterDateTo,
            @Param("dateTo") Instant dateTo,
            @Param("filterUserId") boolean filterUserId,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(sr) FROM SaleRefund sr
            JOIN sr.sale s
            LEFT JOIN s.customer c
            WHERE (:filterStatus = false OR sr.status = :status)
            AND (:filterSaleId = false OR s.id = :saleId)
            AND (:filterQ = false
                 OR LOWER(sr.refundNumber) LIKE CONCAT('%', :q, '%')
                 OR LOWER(s.saleNumber) LIKE CONCAT('%', :q, '%')
                 OR LOWER(c.firstName) LIKE CONCAT('%', :q, '%')
                 OR LOWER(c.lastName) LIKE CONCAT('%', :q, '%')
                 OR LOWER(c.customerNumber) LIKE CONCAT('%', :q, '%'))
            AND (:filterDateFrom = false OR sr.createdAt >= :dateFrom)
            AND (:filterDateTo = false OR sr.createdAt <= :dateTo)
            AND (:filterUserId = false
                 OR sr.cashier.id = :userId
                 OR s.seller.id = :userId
                 OR s.cashier.id = :userId)
            """)
    long countBrowseReturns(
            @Param("filterStatus") boolean filterStatus,
            @Param("status") SaleRefundStatus status,
            @Param("filterSaleId") boolean filterSaleId,
            @Param("saleId") Long saleId,
            @Param("filterQ") boolean filterQ,
            @Param("q") String q,
            @Param("filterDateFrom") boolean filterDateFrom,
            @Param("dateFrom") Instant dateFrom,
            @Param("filterDateTo") boolean filterDateTo,
            @Param("dateTo") Instant dateTo,
            @Param("filterUserId") boolean filterUserId,
            @Param("userId") Long userId);
}
