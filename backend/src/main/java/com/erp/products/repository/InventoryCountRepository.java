package com.erp.products.repository;

import com.erp.products.domain.entity.InventoryCount;
import com.erp.products.domain.enums.InventoryCountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long>, JpaSpecificationExecutor<InventoryCount> {

    Optional<InventoryCount> findByReference(String reference);

    Optional<InventoryCount> findByInventoryNumber(String inventoryNumber);

    long countByInventoryNumberStartingWith(String prefix);

    List<InventoryCount> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    List<InventoryCount> findByStatusOrderByCreatedAtDesc(InventoryCountStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM InventoryCount c WHERE c.id = :id")
    Optional<InventoryCount> findByIdForUpdate(@Param("id") Long id);
}
