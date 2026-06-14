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

    @Query("SELECT COALESCE(SUM(s.quantityOnHand), 0) FROM StockItem s WHERE s.product.id = :productId")
    BigDecimal sumQuantityOnHandByProductId(@Param("productId") Long productId);

    @Query("""
            SELECT COALESCE(SUM(s.quantityOnHand - s.quantityReserved), 0) FROM StockItem s
            WHERE s.product.id = :productId
              AND ((:variantId IS NULL AND s.variant IS NULL) OR s.variant.id = :variantId)
            """)
    BigDecimal sumAvailableByProductAndVariant(@Param("productId") Long productId, @Param("variantId") Long variantId);

    @Query("SELECT COALESCE(SUM(s.quantityOnHand), 0) FROM StockItem s")
    BigDecimal sumTotalQuantityOnHand();

    @Query("""
            SELECT COALESCE(SUM(s.quantityOnHand), 0) FROM StockItem s
            JOIN s.product p
            WHERE p.statut = com.erp.products.domain.enums.ProductStatus.ACTIF
              AND p.cycleVie = com.erp.products.domain.enums.LifecycleStatus.ACTIF
              AND (s.variant IS NULL OR (s.variant.isActive = TRUE AND s.variant.isStockable = TRUE))
            """)
    BigDecimal sumEligibleQuantityOnHand();

    @Query("""
            SELECT COALESCE(SUM(
                s.quantityOnHand *
                CASE WHEN :salePrice = TRUE
                    THEN COALESCE(v.prix, p.prixVente)
                    ELSE COALESCE(v.costPrice, p.prixAchat)
                END
            ), 0)
            FROM StockItem s
            JOIN s.product p
            LEFT JOIN s.variant v ON v.id = s.variant.id
            WHERE p.statut = com.erp.products.domain.enums.ProductStatus.ACTIF
              AND p.cycleVie = com.erp.products.domain.enums.LifecycleStatus.ACTIF
              AND (v IS NULL OR (v.isActive = TRUE AND v.isStockable = TRUE))
              AND (
                (:salePrice = TRUE AND COALESCE(v.prix, p.prixVente) IS NOT NULL
                    AND COALESCE(v.prix, p.prixVente) > 0)
                OR (:salePrice = FALSE AND COALESCE(v.costPrice, p.prixAchat) IS NOT NULL
                    AND COALESCE(v.costPrice, p.prixAchat) > 0)
              )
            """)
    BigDecimal sumEligibleStockValue(@Param("salePrice") boolean salePrice);

    @Query("""
            SELECT p.id, COALESCE(SUM(s.quantityOnHand - s.quantityReserved), 0)
            FROM Product p
            LEFT JOIN StockItem s ON s.product.id = p.id
            WHERE p.statut = com.erp.products.domain.enums.ProductStatus.ACTIF
              AND p.cycleVie = com.erp.products.domain.enums.LifecycleStatus.ACTIF
            GROUP BY p.id
            """)
    List<Object[]> sumAvailableStockPerProduct();

    @Query("""
            SELECT p.id, p.nom, p.sku, COALESCE(SUM(s.quantityOnHand - s.quantityReserved), 0)
            FROM Product p
            LEFT JOIN StockItem s ON s.product.id = p.id
            WHERE p.statut = com.erp.products.domain.enums.ProductStatus.ACTIF
              AND p.cycleVie = com.erp.products.domain.enums.LifecycleStatus.ACTIF
            GROUP BY p.id, p.nom, p.sku
            ORDER BY p.nom
            """)
    List<Object[]> findProductAvailableQuantities();

    @Query("""
            SELECT s.warehouse.id, s.warehouse.code, s.warehouse.nom,
                   COALESCE(SUM(s.quantityOnHand), 0),
                   COALESCE(SUM(
                       s.quantityOnHand *
                       CASE WHEN :salePrice = TRUE
                           THEN COALESCE(v.prix, p.prixVente)
                           ELSE COALESCE(v.costPrice, p.prixAchat)
                       END
                   ), 0)
            FROM StockItem s
            JOIN s.product p
            LEFT JOIN s.variant v ON v.id = s.variant.id
            WHERE p.statut = com.erp.products.domain.enums.ProductStatus.ACTIF
              AND p.cycleVie = com.erp.products.domain.enums.LifecycleStatus.ACTIF
              AND (v IS NULL OR (v.isActive = TRUE AND v.isStockable = TRUE))
              AND (
                (:salePrice = TRUE AND COALESCE(v.prix, p.prixVente) IS NOT NULL
                    AND COALESCE(v.prix, p.prixVente) > 0)
                OR (:salePrice = FALSE AND COALESCE(v.costPrice, p.prixAchat) IS NOT NULL
                    AND COALESCE(v.costPrice, p.prixAchat) > 0)
              )
            GROUP BY s.warehouse.id, s.warehouse.code, s.warehouse.nom
            ORDER BY s.warehouse.nom
            """)
    List<Object[]> summarizeEligibleByWarehouse(@Param("salePrice") boolean salePrice);
}
