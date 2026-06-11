package com.erp.products.repository;

import com.erp.products.domain.entity.ProductCustomAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCustomAttributeValueRepository extends JpaRepository<ProductCustomAttributeValue, Long> {

    List<ProductCustomAttributeValue> findByProductId(Long productId);

    boolean existsByAttributeId(Long attributeId);
}
