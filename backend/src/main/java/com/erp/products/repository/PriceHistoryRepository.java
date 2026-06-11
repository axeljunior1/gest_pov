package com.erp.products.repository;

import com.erp.products.domain.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByProductIdOrderByDateModificationDesc(Long productId);

    List<PriceHistory> findByVariantIdOrderByDateModificationDesc(Long variantId);

    @Modifying
    @Query("DELETE FROM PriceHistory ph WHERE ph.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("DELETE FROM PriceHistory ph WHERE ph.variant.id = :variantId")
    void deleteByVariantId(@Param("variantId") Long variantId);
}
