package com.erp.products.repository;

import com.erp.products.domain.entity.StockValuationMovement;
import com.erp.products.domain.enums.StockValuationMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface StockValuationMovementRepository extends JpaRepository<StockValuationMovement, Long> {

    @Query("""
            SELECT COALESCE(SUM(m.totalValue), 0) FROM StockValuationMovement m
            WHERE m.movementDate <= :at
            AND m.movementType IN ('PURCHASE', 'RETURN', 'INVENTORY')
            """)
    BigDecimal sumInboundValueUntil(@Param("at") Instant at);

    List<StockValuationMovement> findByProductIdAndMovementDateBetweenOrderByMovementDateAsc(
            Long productId, Instant from, Instant to);

    List<StockValuationMovement> findByMovementDateBeforeOrderByMovementDateAscIdAsc(Instant before);

    @Query("""
            SELECT m FROM StockValuationMovement m
            WHERE m.sourceId = :sourceId
            AND m.sourceType = :sourceType
            AND m.product.id = :productId
            AND ((:variantId IS NULL AND m.variant IS NULL) OR m.variant.id = :variantId)
            AND m.quantity < 0
            ORDER BY m.movementDate DESC, m.id DESC
            """)
    java.util.Optional<StockValuationMovement> findLatestOutboundBySource(
            @Param("sourceId") Long sourceId,
            @Param("sourceType") String sourceType,
            @Param("productId") Long productId,
            @Param("variantId") Long variantId);

    @Query(value = """
            SELECT DATE(m.movement_date) AS day,
                   MAX(m.stock_value_after) AS end_value
            FROM stock_valuation_movements m
            WHERE m.movement_date >= :from AND m.movement_date < :to
            GROUP BY DATE(m.movement_date)
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> aggregateDailyEndValues(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT DATE_TRUNC('month', m.movement_date) AS month,
                   SUM(CASE WHEN m.quantity > 0 THEN m.total_value ELSE -m.total_value END) AS net_change
            FROM stock_valuation_movements m
            WHERE m.movement_date >= :from AND m.movement_date < :to
            GROUP BY DATE_TRUNC('month', m.movement_date)
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> aggregateMonthlyNetChange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT p.categorie.id, p.categorie.nom, SUM(v.stockValue)
            FROM StockValuation v
            JOIN v.product p
            WHERE v.stockValue <> 0
            GROUP BY p.categorie.id, p.categorie.nom
            ORDER BY SUM(v.stockValue) DESC
            """)
    List<Object[]> sumValueByCategory();

    @Query(value = """
            SELECT v.product_id, v.variant_id, p.nom, SUM(v.stock_value) AS total_value
            FROM stock_valuation v
            JOIN products p ON p.id = v.product_id
            WHERE v.stock_value > 0
            GROUP BY v.product_id, v.variant_id, p.nom
            ORDER BY total_value DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topProductsByValue(@Param("limit") int limit);

    @Query(value = """
            SELECT v.product_id, v.variant_id, p.nom, v.quantity_on_hand, v.stock_value,
                   COALESCE(MAX(m.movement_date), v.updated_at) AS last_movement
            FROM stock_valuation v
            JOIN products p ON p.id = v.product_id
            LEFT JOIN stock_valuation_movements m ON m.product_id = v.product_id
                AND COALESCE(m.variant_id, 0) = COALESCE(v.variant_id, 0)
            WHERE v.quantity_on_hand > 0
            GROUP BY v.product_id, v.variant_id, p.nom, v.quantity_on_hand, v.stock_value, v.updated_at
            HAVING COALESCE(MAX(m.movement_date), v.updated_at) < :before
            ORDER BY last_movement ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> staleProducts(@Param("before") Instant before, @Param("limit") int limit);
}
