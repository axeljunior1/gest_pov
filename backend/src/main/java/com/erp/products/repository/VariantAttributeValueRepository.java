package com.erp.products.repository;

import com.erp.products.domain.entity.VariantAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VariantAttributeValueRepository extends JpaRepository<VariantAttributeValue, Long> {

    List<VariantAttributeValue> findByAttributeIdOrderBySortOrderAscValueAsc(Long attributeId);

    List<VariantAttributeValue> findByAttributeIdAndIsActiveTrueOrderBySortOrderAscValueAsc(Long attributeId);

    Optional<VariantAttributeValue> findByAttributeIdAndValueIgnoreCase(Long attributeId, String value);

    boolean existsByAttributeId(Long attributeId);
}
