package com.erp.products.repository;

import com.erp.products.domain.entity.SaleLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleLineRepository extends JpaRepository<SaleLine, Long> {

    @Query("SELECT COUNT(sl) > 0 FROM SaleLine sl WHERE sl.variant.id = :variantId")
    boolean existsByVariantId(@Param("variantId") Long variantId);
}
