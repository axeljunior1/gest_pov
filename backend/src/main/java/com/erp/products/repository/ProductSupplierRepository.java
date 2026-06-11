package com.erp.products.repository;

import com.erp.products.domain.entity.ProductSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, Long> {

    List<ProductSupplier> findByProductId(Long productId);

    long countBySupplierId(Long supplierId);
}
