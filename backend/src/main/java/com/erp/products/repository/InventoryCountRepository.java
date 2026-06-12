package com.erp.products.repository;

import com.erp.products.domain.entity.InventoryCount;
import com.erp.products.domain.enums.InventoryCountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {

    Optional<InventoryCount> findByReference(String reference);

    List<InventoryCount> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    List<InventoryCount> findByStatusOrderByCreatedAtDesc(InventoryCountStatus status);
}
