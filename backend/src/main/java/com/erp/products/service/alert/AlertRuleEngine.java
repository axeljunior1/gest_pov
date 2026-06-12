package com.erp.products.service.alert;

import com.erp.products.domain.entity.InventoryCountLine;
import com.erp.products.domain.entity.Product;
import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AlertRuleEngine {

    private final StockAlertChecker stockAlertChecker;
    private final ExpiryAlertChecker expiryAlertChecker;
    private final DormantProductChecker dormantProductChecker;
    private final SupplierDelayChecker supplierDelayChecker;
    private final AlertService alertService;

    @Transactional
    public void afterStockMovement(
            Long productId,
            Long variantId,
            Long warehouseId,
            Long locationId,
            Long lotId) {
        stockAlertChecker.checkAfterMovement(productId, variantId, warehouseId, locationId, lotId);
        if (lotId != null) {
            expiryAlertChecker.checkLot(lotId, warehouseId, locationId);
        }
    }

    @Transactional
    public void runScheduledChecks() {
        expiryAlertChecker.checkAll();
        dormantProductChecker.checkAll();
        supplierDelayChecker.checkAll();
    }

    @Transactional
    public void onInventoryDiscrepancy(InventoryCountLine line, String inventoryReference) {
        if (line.getEcart().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        Product product = line.getProduct();
        AlertService.AlertPosition position = new AlertService.AlertPosition(
                product, null, line.getLocation(), line.getLot());

        alertService.triggerIfNeeded(
                AlertType.INVENTORY_DISCREPANCY,
                AlertSeverity.WARNING,
                position,
                line.getEcart(),
                line.getQuantitySystem(),
                "Ecart inventaire " + inventoryReference + " pour " + product.getNom()
                        + " : systeme=" + line.getQuantitySystem()
                        + " compte=" + line.getQuantityCounted());
    }
}
