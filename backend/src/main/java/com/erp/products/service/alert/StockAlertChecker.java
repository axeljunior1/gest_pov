package com.erp.products.service.alert;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.repository.StockItemRepository;
import com.erp.products.service.ProductVariantPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class StockAlertChecker {

    private final StockItemRepository stockItemRepository;
    private final AlertSettingResolver settingResolver;
    private final AlertService alertService;
    private final ProductVariantPolicyService variantPolicyService;

    public void checkAfterMovement(
            Long productId,
            Long variantId,
            Long warehouseId,
            Long locationId,
            Long lotId) {
        StockItem item = loadStockItem(productId, variantId, warehouseId, locationId, lotId);
        if (item == null) {
            return;
        }
        checkItem(item);
    }

    public void checkItem(StockItem item) {
        BigDecimal available = item.getQuantityAvailable();
        var settings = settingResolver.resolve(
                item.getProduct().getId(),
                item.getWarehouse().getId());

        AlertService.AlertPosition position = new AlertService.AlertPosition(
                item.getProduct(), item.getWarehouse(), item.getLocation(), item.getLot());

        String itemLabel = formatStockItemLabel(item);

        boolean outOfStock = available.compareTo(BigDecimal.ZERO) <= 0;
        boolean lowStock = !outOfStock
                && settings.minStockLevel() != null
                && available.compareTo(settings.minStockLevel()) <= 0;
        boolean overstock = settings.maxStockLevel() != null
                && available.compareTo(settings.maxStockLevel()) >= 0;

        if (outOfStock) {
            alertService.triggerIfNeeded(
                    AlertType.OUT_OF_STOCK,
                    AlertSeverity.CRITICAL,
                    position,
                    available,
                    BigDecimal.ZERO,
                    "Rupture de stock pour " + itemLabel
                            + " (" + item.getWarehouse().getCode() + "/" + item.getLocation().getCode() + ")");
            alertService.autoResolveIfOpen(AlertType.LOW_STOCK, position);
            alertService.autoResolveIfOpen(AlertType.OVERSTOCK, position);
        } else {
            alertService.autoResolveIfOpen(AlertType.OUT_OF_STOCK, position);
        }

        if (lowStock) {
            alertService.triggerIfNeeded(
                    AlertType.LOW_STOCK,
                    AlertSeverity.WARNING,
                    position,
                    available,
                    settings.minStockLevel(),
                    "Stock faible pour " + itemLabel
                            + " : " + available.stripTrailingZeros().toPlainString()
                            + " (seuil " + settings.minStockLevel().stripTrailingZeros().toPlainString() + ")");
        } else {
            alertService.autoResolveIfOpen(AlertType.LOW_STOCK, position);
        }

        if (overstock) {
            alertService.triggerIfNeeded(
                    AlertType.OVERSTOCK,
                    AlertSeverity.INFO,
                    position,
                    available,
                    settings.maxStockLevel(),
                    "Surstock pour " + itemLabel
                            + " : " + available.stripTrailingZeros().toPlainString());
        } else {
            alertService.autoResolveIfOpen(AlertType.OVERSTOCK, position);
        }
    }

    private String formatStockItemLabel(StockItem item) {
        if (item.getVariant() != null) {
            return item.getProduct().getNom() + " — " + variantPolicyService.buildVariantName(item.getVariant());
        }
        return item.getProduct().getNom();
    }

    private StockItem loadStockItem(
            Long productId,
            Long variantId,
            Long warehouseId,
            Long locationId,
            Long lotId) {
        Long lotKey = lotId != null ? lotId : 0L;
        return stockItemRepository.findByPosition(productId, variantId, warehouseId, locationId, lotKey)
                .orElse(null);
    }
}
