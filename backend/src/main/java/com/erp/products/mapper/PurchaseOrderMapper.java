package com.erp.products.mapper;

import com.erp.products.domain.entity.AlertSetting;
import com.erp.products.domain.entity.SupplierPurchaseOrder;
import com.erp.products.domain.entity.SupplierPurchaseOrderLine;
import com.erp.products.dto.AlertSettingResponse;
import com.erp.products.dto.PurchaseOrderResponse;
import com.erp.products.service.ProductVariantAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PurchaseOrderMapper {

    private final ProductVariantAttributeService variantAttributeService;

    public PurchaseOrderResponse toResponse(SupplierPurchaseOrder order) {
        PurchaseOrderResponse.PurchaseOrderResponseBuilder builder = PurchaseOrderResponse.builder()
                .id(order.getId())
                .reference(order.getReference())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .status(order.getStatus())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt());

        if (order.getSupplier() != null) {
            builder.supplierId(order.getSupplier().getId())
                    .supplierNom(order.getSupplier().getNom());
        }
        if (order.getWarehouse() != null) {
            builder.warehouseId(order.getWarehouse().getId())
                    .warehouseCode(order.getWarehouse().getCode());
        }
        if (order.getStockEntry() != null) {
            builder.stockEntryId(order.getStockEntry().getId())
                    .stockEntryNumber(order.getStockEntry().getEntryNumber());
        }
        if (order.getLines() != null) {
            builder.lines(order.getLines().stream().map(this::toLineResponse).toList());
        }
        return builder.build();
    }

    private PurchaseOrderResponse.LineResponse toLineResponse(SupplierPurchaseOrderLine line) {
        BigDecimal received = line.getReceivedQuantity() != null ? line.getReceivedQuantity() : BigDecimal.ZERO;
        BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
        PurchaseOrderResponse.LineResponse.LineResponseBuilder builder = PurchaseOrderResponse.LineResponse.builder()
                .id(line.getId())
                .quantity(qty)
                .receivedQuantity(received)
                .remainingQuantity(qty.subtract(received).max(BigDecimal.ZERO))
                .unitPrice(line.getUnitPrice())
                .notes(line.getNotes());

        if (line.getProduct() != null) {
            builder.productId(line.getProduct().getId())
                    .productNom(line.getProduct().getNom())
                    .productSku(line.getProduct().getSku());
        }
        if (line.getVariant() != null) {
            builder.variantId(line.getVariant().getId())
                    .variantLabel(variantAttributeService.buildVariantLabel(line.getVariant()));
        }
        return builder.build();
    }

    public AlertSettingResponse toAlertSettingResponse(AlertSetting setting) {
        AlertSettingResponse.AlertSettingResponseBuilder builder = AlertSettingResponse.builder()
                .id(setting.getId())
                .scope(setting.getScope())
                .minStockLevel(setting.getMinStockLevel())
                .maxStockLevel(setting.getMaxStockLevel())
                .expiryAlertDays(setting.getExpiryAlertDays())
                .dormantDays(setting.getDormantDays())
                .actif(setting.getActif())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt());

        if (setting.getProduct() != null) {
            builder.productId(setting.getProduct().getId())
                    .productNom(setting.getProduct().getNom())
                    .productSku(setting.getProduct().getSku());
        }
        if (setting.getWarehouse() != null) {
            builder.warehouseId(setting.getWarehouse().getId())
                    .warehouseCode(setting.getWarehouse().getCode())
                    .warehouseNom(setting.getWarehouse().getNom());
        }
        return builder.build();
    }
}
