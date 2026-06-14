package com.erp.products.repository;

import com.erp.products.domain.entity.Sale;
import com.erp.products.domain.enums.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    long countBySaleNumberStartingWith(String prefix);

    Optional<Sale> findBySaleNumber(String saleNumber);

    List<Sale> findByPosSessionIdAndStatusOrderByCreatedAtDesc(Long sessionId, SaleStatus status);

    List<Sale> findBySellerIdAndWarehouseIdAndStatusOrderByCreatedAtDesc(
            Long sellerId, Long warehouseId, SaleStatus status);

    List<Sale> findByPosSessionIdOrderByCreatedAtDesc(Long sessionId);

    List<Sale> findByPaymentSessionIdOrderByCreatedAtDesc(Long paymentSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT sl.product.id, SUM(sl.quantityInBaseUnit)
            FROM SaleLine sl
            JOIN sl.sale s
            WHERE s.status IN (com.erp.products.domain.enums.SaleStatus.PAID,
                               com.erp.products.domain.enums.SaleStatus.VALIDATED)
            GROUP BY sl.product.id
            ORDER BY SUM(sl.quantityInBaseUnit) DESC
            """)
    List<Object[]> findTopSoldProductIds(org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT COUNT(s), COALESCE(SUM(s.total), 0), MAX(s.validatedAt)
            FROM Sale s
            WHERE s.customer.id = :customerId
            AND s.status IN (
                com.erp.products.domain.enums.SaleStatus.PAID,
                com.erp.products.domain.enums.SaleStatus.VALIDATED,
                com.erp.products.domain.enums.SaleStatus.PARTIALLY_REFUNDED,
                com.erp.products.domain.enums.SaleStatus.REFUNDED
            )
            """)
    Object[] aggregateCustomerPurchases(@Param("customerId") Long customerId);

    @Query("""
            SELECT sl.product.id, sl.product.nom, SUM(sl.quantityInput), SUM(sl.lineTotal)
            FROM SaleLine sl
            JOIN sl.sale s
            WHERE s.customer.id = :customerId
            AND s.status IN (
                com.erp.products.domain.enums.SaleStatus.PAID,
                com.erp.products.domain.enums.SaleStatus.VALIDATED,
                com.erp.products.domain.enums.SaleStatus.PARTIALLY_REFUNDED,
                com.erp.products.domain.enums.SaleStatus.REFUNDED
            )
            GROUP BY sl.product.id, sl.product.nom
            ORDER BY SUM(sl.lineTotal) DESC
            """)
    List<Object[]> findTopProductsByCustomer(@Param("customerId") Long customerId,
                                               org.springframework.data.domain.Pageable pageable);

    List<Sale> findByCustomerIdAndStatusInOrderByValidatedAtDesc(
            Long customerId, List<SaleStatus> statuses, org.springframework.data.domain.Pageable pageable);

    List<Sale> findByStatusAndWarehouseIdOrderBySubmittedAtAsc(SaleStatus status, Long warehouseId);

    List<Sale> findByStatusOrderBySubmittedAtAsc(SaleStatus status);

    long countByStatusAndWarehouseId(SaleStatus status, Long warehouseId);

    @Query("""
            SELECT s FROM Sale s
            WHERE s.status = com.erp.products.domain.enums.SaleStatus.PAID
            AND NOT EXISTS (SELECT 1 FROM StockExit e WHERE e.sale.id = s.id)
            ORDER BY s.paidAt ASC NULLS LAST, s.id ASC
            """)
    List<Sale> findPaidWithoutStockExit(org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT s FROM Sale s
            WHERE s.status IN :statuses
            AND (:sessionId IS NULL
                 OR s.posSession.id = :sessionId
                 OR s.paymentSession.id = :sessionId)
            AND (:userId IS NULL OR s.seller.id = :userId OR s.cashier.id = :userId)
            AND (:sellerId IS NULL OR s.seller.id = :sellerId)
            AND (:cashierId IS NULL OR s.cashier.id = :cashierId)
            ORDER BY COALESCE(s.paidAt, s.validatedAt) DESC, s.id DESC
            """)
    List<Sale> findCompletedSales(
            @Param("statuses") Collection<SaleStatus> statuses,
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId,
            @Param("sellerId") Long sellerId,
            @Param("cashierId") Long cashierId,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT s FROM Sale s
            LEFT JOIN s.customer c
            WHERE s.status IN :statuses
            AND (
                LOWER(s.saleNumber) LIKE CONCAT('%', :q, '%')
                OR LOWER(c.firstName) LIKE CONCAT('%', :q, '%')
                OR LOWER(c.lastName) LIKE CONCAT('%', :q, '%')
                OR LOWER(c.customerNumber) LIKE CONCAT('%', :q, '%')
            )
            ORDER BY COALESCE(s.paidAt, s.validatedAt) DESC, s.id DESC
            """)
    List<Sale> searchRefundableSales(
            @Param("statuses") Collection<SaleStatus> statuses,
            @Param("q") String q,
            org.springframework.data.domain.Pageable pageable);
}
