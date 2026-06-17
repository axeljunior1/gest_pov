package com.erp.products.repository;

import com.erp.products.domain.entity.StockValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface StockValuationRepository extends JpaRepository<StockValuation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT v FROM StockValuation v
            WHERE v.product.id = :productId
            AND ((:variantId IS NULL AND v.variant IS NULL) OR v.variant.id = :variantId)
            """)
    Optional<StockValuation> findForUpdate(
            @Param("productId") Long productId,
            @Param("variantId") Long variantId);

    @Query("""
            SELECT v FROM StockValuation v
            WHERE v.product.id = :productId
            AND ((:variantId IS NULL AND v.variant IS NULL) OR v.variant.id = :variantId)
            """)
    Optional<StockValuation> findByProductAndVariant(
            @Param("productId") Long productId,
            @Param("variantId") Long variantId);

    @Query("SELECT COALESCE(SUM(v.stockValue), 0) FROM StockValuation v WHERE v.quantityOnHand <> 0 OR v.stockValue <> 0")
    BigDecimal sumTotalStockValue();

    @Query("""
            SELECT v FROM StockValuation v
            JOIN FETCH v.product p
            LEFT JOIN FETCH v.variant
            WHERE v.quantityOnHand > 0 OR v.stockValue <> 0
            ORDER BY v.stockValue DESC
            """)
    List<StockValuation> findAllWithStock();
}
