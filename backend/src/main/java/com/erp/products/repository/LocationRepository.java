package com.erp.products.repository;

import com.erp.products.domain.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByWarehouseIdAndActifTrueOrderByCodeAsc(Long warehouseId);

    Optional<Location> findByIdAndWarehouseId(Long id, Long warehouseId);

    Optional<Location> findByWarehouseIdAndCode(Long warehouseId, String code);
}
