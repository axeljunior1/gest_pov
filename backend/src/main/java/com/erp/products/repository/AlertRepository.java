package com.erp.products.repository;

import com.erp.products.domain.entity.Alert;
import com.erp.products.domain.enums.AlertStatus;
import com.erp.products.domain.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    Optional<Alert> findByTypeAndProductIdAndWarehouseIdAndLocationIdAndLotKeyAndStatus(
            AlertType type,
            Long productId,
            Long warehouseId,
            Long locationId,
            Long lotKey,
            AlertStatus status);

    List<Alert> findByStatusOrderByLastTriggeredAtDesc(AlertStatus status);

    List<Alert> findByProductIdOrderByLastTriggeredAtDesc(Long productId);

    List<Alert> findAllByOrderByLastTriggeredAtDesc();

    long countByStatus(AlertStatus status);

    long countByStatusAndType(AlertStatus status, AlertType type);
}
