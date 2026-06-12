package com.erp.products.repository;

import com.erp.products.domain.entity.StockItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    @Query("""
            SELECT s FROM StockItem s
            WHERE s.product.id = :productId
              AND ((:variantId IS NULL AND s.variant IS NULL) OR s.variant.id = :variantId)
              AND s.warehouse.id = :warehouseId
              AND s.location.id = :locationId
              AND s.lotKey = :lotKey
            """)
    Optional<StockItem> findByPosition(
            @Param("productId") Long productId,
            @Param("variantId") Long variantId,
            @Param("warehouseId") Long warehouseId,
            @Param("locationId") Long locationId,
            @Param("lotKey") Long lotKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM StockItem s
            WHERE s.product.id = :productId
              AND ((:variantId IS NULL AND s.variant IS NULL) OR s.variant.id = :variantId)
              AND s.warehouse.id = :warehouseId
              AND s.location.id = :locationId
              AND s.lotKey = :lotKey
            """)
    Optional<StockItem> findForUpdate(
            @Param("productId") Long productId,
            @Param("variantId") Long variantId,
            @Param("warehouseId") Long warehouseId,
            @Param("locationId") Long locationId,
            @Param("lotKey") Long lotKey);

    List<StockItem> findByProductId(Long productId);

    List<StockItem> findByVariantId(Long variantId);

    List<StockItem> findByWarehouseId(Long warehouseId);

    @Query("SELECT COALESCE(SUM(s.quantityOnHand), 0) FROM StockItem s WHERE s.variant.id = :variantId")
    BigDecimal sumQuantityOnHandByVariantId(@Param("variantId") Long variantId);

    @Query("""
            SELECT COALESCE(SUM(s.quantityOnHand - s.quantityReserved), 0) FROM StockItem s
            WHERE s.product.id = :productId
              AND ((:variantId IS NULL AND s.variant IS NULL) OR s.variant.id = :variantId)
            """)
    BigDecimal sumAvailableByProductAndVariant(@Param("productId") Long productId, @Param("variantId") Long variantId);
}
