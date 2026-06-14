package com.erp.products.repository;

import com.erp.products.domain.entity.VariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VariantAttributeRepository extends JpaRepository<VariantAttribute, Long> {

    Optional<VariantAttribute> findByCode(String code);

    boolean existsByCode(String code);
}
