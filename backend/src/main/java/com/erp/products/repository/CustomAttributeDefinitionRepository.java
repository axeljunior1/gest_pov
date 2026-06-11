package com.erp.products.repository;

import com.erp.products.domain.entity.CustomAttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomAttributeDefinitionRepository extends JpaRepository<CustomAttributeDefinition, Long> {

    Optional<CustomAttributeDefinition> findByCode(String code);
}
