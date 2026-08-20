package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO client — miroir de ProductResponse. Stock, unicité SKU et cycle de vie restent côté API.
 */
public record Product(
        Long id,
        String nom,
        String sku,
        String codeBarre,
        String description,
        Long marqueId,
        String marque,
        Long categorieId,
        String categorieNom,
        BigDecimal prixAchat,
        BigDecimal prixVente,
        BigDecimal prixPromotionnel,
        Long fournisseurPrincipalId,
        String fournisseurPrincipalNom,
        Long unitId,
        String unitSymbole,
        String baseUnitSymbole,
        String statut,
        String cycleVie,
        boolean hasVariants,
        Integer stockTotal,
        List<ProductImage> images,
        List<ProductVariant> variantes,
        String createdAt,
        String updatedAt
) {

    public static Product fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        List<ProductImage> images = new ArrayList<>();
        JsonNode imagesNode = node.get("images");
        if (imagesNode != null && imagesNode.isArray()) {
            imagesNode.forEach(item -> {
                ProductImage image = ProductImage.fromJson(item);
                if (image != null) {
                    images.add(image);
                }
            });
        }
        List<ProductVariant> variantes = new ArrayList<>();
        JsonNode variantesNode = node.get("variantes");
        if (variantesNode != null && variantesNode.isArray()) {
            variantesNode.forEach(item -> {
                ProductVariant variant = ProductVariant.fromJson(item);
                if (variant != null) {
                    variantes.add(variant);
                }
            });
        }
        return new Product(
                longOrNull(node, "id"),
                node.path("nom").asText(""),
                textOrNull(node, "sku"),
                textOrNull(node, "codeBarre"),
                textOrNull(node, "description"),
                longOrNull(node, "marqueId"),
                textOrNull(node, "marque"),
                longOrNull(node, "categorieId"),
                textOrNull(node, "categorieNom"),
                decimalOrNull(node, "prixAchat"),
                decimalOrNull(node, "prixVente"),
                decimalOrNull(node, "prixPromotionnel"),
                longOrNull(node, "fournisseurPrincipalId"),
                textOrNull(node, "fournisseurPrincipalNom"),
                longOrNull(node, "unitId"),
                textOrNull(node, "unitSymbole"),
                textOrNull(node, "baseUnitSymbole"),
                textOrNull(node, "statut"),
                textOrNull(node, "cycleVie"),
                node.path("hasVariants").asBoolean(false) || !variantes.isEmpty(),
                node.hasNonNull("stockTotal") ? node.get("stockTotal").asInt() : 0,
                List.copyOf(images),
                List.copyOf(variantes),
                textOrNull(node, "createdAt"),
                textOrNull(node, "updatedAt")
        );
    }

    public String stockLabel() {
        int stock = stockTotal == null ? 0 : stockTotal;
        String unit = baseUnitSymbole != null && !baseUnitSymbole.isBlank()
                ? baseUnitSymbole
                : (unitSymbole == null ? "" : unitSymbole);
        return unit.isBlank() ? String.valueOf(stock) : stock + " " + unit;
    }

    static Long longOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asLong() : null;
    }

    static String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    static BigDecimal decimalOrNull(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        try {
            return new BigDecimal(node.get(field).asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
