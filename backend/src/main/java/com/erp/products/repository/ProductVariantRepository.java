package com.erp.products.repository;

import com.erp.products.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    long countByProductId(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    Optional<ProductVariant> findByCodeBarre(String codeBarre);

    @Query("""
            SELECT v FROM ProductVariant v
            WHERE LOWER(TRIM(v.codeBarre)) = LOWER(TRIM(:barcode))
            """)
    Optional<ProductVariant> findByCodeBarreNormalized(@Param("barcode") String barcode);

    @Query("""
            SELECT v FROM ProductVariant v
            WHERE LOWER(TRIM(v.codeBarre)) = LOWER(TRIM(:barcode))
            AND v.isActive = true
            AND v.isSellable = true
            """)
    Optional<ProductVariant> findActiveSellableByCodeBarre(@Param("barcode") String barcode);

    boolean existsBySku(String sku);
}
