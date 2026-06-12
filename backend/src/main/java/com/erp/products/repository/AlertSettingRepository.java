package com.erp.products.repository;

import com.erp.products.domain.entity.AlertSetting;
import com.erp.products.domain.enums.AlertSettingScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertSettingRepository extends JpaRepository<AlertSetting, Long> {

    Optional<AlertSetting> findByScopeAndActifTrue(AlertSettingScope scope);

    Optional<AlertSetting> findByScopeAndProductIdAndActifTrue(AlertSettingScope scope, Long productId);

    Optional<AlertSetting> findByScopeAndWarehouseIdAndActifTrue(AlertSettingScope scope, Long warehouseId);

    @Query("""
            SELECT s FROM AlertSetting s
            WHERE s.scope = com.erp.products.domain.enums.AlertSettingScope.PRODUCT_WAREHOUSE
              AND s.actif = true
              AND s.product.id = :productId
              AND s.warehouse.id = :warehouseId
            """)
    Optional<AlertSetting> findProductWarehouseSetting(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    List<AlertSetting> findByActifTrue();
}
