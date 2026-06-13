package com.erp.products.repository;

import com.erp.products.domain.entity.AppReferenceValue;
import com.erp.products.domain.enums.ReferenceValueCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppReferenceValueRepository extends JpaRepository<AppReferenceValue, Long> {

    List<AppReferenceValue> findByCategoryAndActiveTrueOrderBySortOrderAscLabelAsc(ReferenceValueCategory category);

    List<AppReferenceValue> findByActiveTrueOrderByCategoryAscSortOrderAscLabelAsc();

    Optional<AppReferenceValue> findByCategoryAndCodeIgnoreCase(ReferenceValueCategory category, String code);

    boolean existsByCategoryAndCodeIgnoreCaseAndActiveTrue(ReferenceValueCategory category, String code);
}
