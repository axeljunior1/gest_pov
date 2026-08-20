package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record PosProduct(
        Long id,
        String nom,
        String sku,
        BigDecimal unitPrice,
        BigDecimal stockAvailable,
        boolean hasVariants,
        Long matchedVariantId,
        Long matchedPackagingId,
        List<PosVariant> variants,
        List<PosPackaging> packagings
) {
    public record PosVariant(Long id, String label, BigDecimal unitPrice, BigDecimal stockAvailable,
                             List<PosPackaging> packagings) {
        static PosVariant fromJson(JsonNode n) {
            if (n == null || n.isNull()) {
                return null;
            }
            return new PosVariant(
                    Product.longOrNull(n, "id"),
                    Product.textOrNull(n, "label"),
                    Product.decimalOrNull(n, "unitPrice"),
                    Product.decimalOrNull(n, "stockAvailable"),
                    mapPackagings(n.get("packagings"))
            );
        }

        @Override
        public String toString() {
            return (label == null ? "#" + id : label) + " — "
                    + (unitPrice == null ? "0" : unitPrice.stripTrailingZeros().toPlainString());
        }
    }

    public record PosPackaging(Long id, String nom, BigDecimal quantiteBase, BigDecimal salePrice) {
        static PosPackaging fromJson(JsonNode n) {
            if (n == null || n.isNull()) {
                return null;
            }
            return new PosPackaging(
                    Product.longOrNull(n, "id"),
                    Product.textOrNull(n, "nom"),
                    Product.decimalOrNull(n, "quantiteBase"),
                    Product.decimalOrNull(n, "salePrice")
            );
        }

        @Override
        public String toString() {
            return (nom == null ? "#" + id : nom)
                    + (salePrice == null ? "" : " — " + salePrice.stripTrailingZeros().toPlainString());
        }
    }

    public static PosProduct fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        List<PosVariant> variants = new ArrayList<>();
        JsonNode vNode = node.get("variants");
        if (vNode != null && vNode.isArray()) {
            for (JsonNode v : vNode) {
                PosVariant pv = PosVariant.fromJson(v);
                if (pv != null) {
                    variants.add(pv);
                }
            }
        }
        return new PosProduct(
                Product.longOrNull(node, "id"),
                node.path("nom").asText(""),
                Product.textOrNull(node, "sku"),
                Product.decimalOrNull(node, "unitPrice"),
                Product.decimalOrNull(node, "stockAvailable"),
                node.path("hasVariants").asBoolean(false),
                Product.longOrNull(node, "matchedVariantId"),
                Product.longOrNull(node, "matchedPackagingId"),
                List.copyOf(variants),
                mapPackagings(node.get("packagings"))
        );
    }

    private static List<PosPackaging> mapPackagings(JsonNode node) {
        List<PosPackaging> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode p : node) {
                PosPackaging pp = PosPackaging.fromJson(p);
                if (pp != null) {
                    list.add(pp);
                }
            }
        }
        return List.copyOf(list);
    }

    public static List<PosProduct> fromSearch(JsonNode node) {
        JsonNode products = node == null ? null : node.get("products");
        return com.gestpov.desktop.net.JsonLists.mapArray(products, PosProduct::fromJson);
    }

    public boolean needsVariantPick() {
        return hasVariants && matchedVariantId == null && variants != null && !variants.isEmpty();
    }

    public boolean needsPackagingPick() {
        return matchedPackagingId == null && packagings != null && packagings.size() > 1;
    }
}
