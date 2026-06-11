package com.erp.products.repository;

import com.erp.products.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    Optional<ProductVariant> findByCodeBarre(String codeBarre);

    boolean existsBySku(String sku);
}
