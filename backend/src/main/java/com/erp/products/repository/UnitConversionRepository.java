package com.erp.products.repository;

import com.erp.products.domain.entity.UnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {

    Optional<UnitConversion> findByFromUnitIdAndToUnitId(Long fromUnitId, Long toUnitId);

    List<UnitConversion> findByFromUnitIdOrToUnitId(Long fromUnitId, Long toUnitId);
}
