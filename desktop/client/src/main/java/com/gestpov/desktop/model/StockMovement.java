package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record StockMovement(
        Long id,
        String movementType,
        Long productId,
        String productNom,
        String warehouseCode,
        String locationCode,
        BigDecimal quantity,
        BigDecimal quantityAfter,
        String reference,
        String createdAt
) {

    public static StockMovement fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String when = Product.textOrNull(node, "createdAt");
        if (when == null) {
            when = Product.textOrNull(node, "movementDate");
        }
        return new StockMovement(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "movementType"),
                Product.longOrNull(node, "productId"),
                Product.textOrNull(node, "productNom"),
                Product.textOrNull(node, "warehouseCode"),
                Product.textOrNull(node, "locationCode"),
                Product.decimalOrNull(node, "quantity"),
                Product.decimalOrNull(node, "quantityAfter"),
                Product.textOrNull(node, "reference"),
                when
        );
    }

    public String typeLabel() {
        if (movementType == null) {
            return "—";
        }
        return switch (movementType) {
            case "RECEIPT", "IN", "ENTRY" -> "Réception";
            case "INITIAL_STOCK" -> "Stock initial";
            case "RETURN_IN" -> "Retour client";
            case "TRANSFER_IN" -> "Transfert entrant";
            case "ISSUE", "OUT", "EXIT" -> "Sortie";
            case "RETURN_OUT" -> "Retour fournisseur";
            case "TRANSFER_OUT" -> "Transfert sortant";
            case "ADJUST", "ADJUSTMENT" -> "Ajustement";
            case "RESERVATION" -> "Réservation";
            case "RELEASE" -> "Libération réservation";
            case "INVENTORY" -> "Comptage inventaire";
            default -> movementType;
        };
    }

    /** Couleur dédiée par type de mouvement — familles vertes (entrée), rouges (sortie), ambre (ajustement). */
    public String typeColor() {
        if (movementType == null) {
            return "#334155";
        }
        return switch (movementType) {
            case "RECEIPT", "IN", "ENTRY" -> "#166534";
            case "INITIAL_STOCK" -> "#065f46";
            case "RETURN_IN" -> "#0f766e";
            case "TRANSFER_IN" -> "#0e7490";
            case "ISSUE", "OUT", "EXIT" -> "#b91c1c";
            case "RETURN_OUT" -> "#c2410c";
            case "TRANSFER_OUT" -> "#be185d";
            case "ADJUST", "ADJUSTMENT" -> "#1e40af";
            case "RESERVATION" -> "#4338ca";
            case "RELEASE" -> "#0369a1";
            case "INVENTORY" -> "#7c3aed";
            default -> "#334155";
        };
    }
}
