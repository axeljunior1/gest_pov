package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record PurchaseOrder(
        Long id,
        String reference,
        String supplierNom,
        String warehouseCode,
        String status,
        String expectedDeliveryDate,
        String createdAt
) {

    public static PurchaseOrder fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new PurchaseOrder(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "reference"),
                Product.textOrNull(node, "supplierNom"),
                Product.textOrNull(node, "warehouseCode"),
                Product.textOrNull(node, "status"),
                Product.textOrNull(node, "expectedDeliveryDate"),
                Product.textOrNull(node, "createdAt")
        );
    }
}
