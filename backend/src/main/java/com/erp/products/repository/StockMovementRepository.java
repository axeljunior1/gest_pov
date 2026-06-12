package com.erp.products.repository;

import com.erp.products.domain.entity.StockMovement;
import com.erp.products.domain.enums.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    List<StockMovement> findByProductIdOrderByMovementDateDescCreatedAtDesc(Long productId);

    List<StockMovement> findByWarehouseIdOrderByMovementDateDescCreatedAtDesc(Long warehouseId);

    List<StockMovement> findByMovementTypeOrderByMovementDateDescCreatedAtDesc(StockMovementType movementType);

    List<StockMovement> findTop200ByOrderByMovementDateDescCreatedAtDesc();

    List<StockMovement> findByReferenceTypeAndReferenceIdOrderByMovementDateDescCreatedAtDesc(
            String referenceType, Long referenceId);

    @Query("SELECT MAX(m.movementDate) FROM StockMovement m WHERE m.product.id = :productId")
    java.util.Optional<java.time.Instant> findLastMovementDateByProductId(@Param("productId") Long productId);

    @Query("""
            SELECT m.product.id, m.product.nom, COUNT(m), COALESCE(SUM(ABS(m.quantity)), 0)
            FROM StockMovement m
            GROUP BY m.product.id, m.product.nom
            ORDER BY COUNT(m) DESC, COALESCE(SUM(ABS(m.quantity)), 0) DESC
            """)
    List<Object[]> findTopMovedProducts(org.springframework.data.domain.Pageable pageable);
}
