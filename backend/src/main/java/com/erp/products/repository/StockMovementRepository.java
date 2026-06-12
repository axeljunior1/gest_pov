package com.erp.products.repository;

import com.erp.products.domain.entity.StockMovement;
import com.erp.products.domain.enums.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByMovementDateDescCreatedAtDesc(Long productId);

    List<StockMovement> findByWarehouseIdOrderByMovementDateDescCreatedAtDesc(Long warehouseId);

    List<StockMovement> findByMovementTypeOrderByMovementDateDescCreatedAtDesc(StockMovementType movementType);

    List<StockMovement> findTop200ByOrderByMovementDateDescCreatedAtDesc();

    @Query("SELECT MAX(m.movementDate) FROM StockMovement m WHERE m.product.id = :productId")
    java.util.Optional<java.time.Instant> findLastMovementDateByProductId(@Param("productId") Long productId);
}
