package com.erp.products.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Génération de SKU produit / variante à partir du nom, couleur et taille.
 */
public final class ProductSkuGenerator {

    private static final Map<String, String> COLOR_ABBREVIATIONS = Map.ofEntries(
            Map.entry("NOIR", "BK"),
            Map.entry("BLANC", "WH"),
            Map.entry("ROUGE", "RD"),
            Map.entry("BLEU", "BL"),
            Map.entry("VERT", "GN"),
            Map.entry("JAUNE", "YL"),
            Map.entry("GRIS", "GY"),
            Map.entry("ROSE", "PK"),
            Map.entry("ORANGE", "OR"),
            Map.entry("MARRON", "BN"),
            Map.entry("BEIGE", "BE"),
            Map.entry("VIOLET", "VT"),
            Map.entry("MARINE", "NV")
    );

    private ProductSkuGenerator() {
    }

    public static String baseFromProductName(String nom) {
        String base = normalizePart(nom, 30);
        return base.isEmpty() ? "PROD" : base;
    }

    public static String variantSuffix(String couleur, String taille, int variantIndex) {
        String colorPart = abbreviateColor(couleur);
        String sizePart = normalizePart(taille, 10);

        if (!colorPart.isEmpty() && !sizePart.isEmpty()) {
            return colorPart + "-" + sizePart;
        }
        if (!colorPart.isEmpty()) {
            return colorPart;
        }
        if (!sizePart.isEmpty()) {
            return sizePart;
        }
        return "V" + Math.max(1, variantIndex);
    }

    public static String ensureUnique(String base, Predicate<String> exists) {
        if (!exists.test(base)) {
            return base;
        }
        for (int i = 2; i < 10_000; i++) {
            String candidate = base + "-" + i;
            if (!exists.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Impossible de generer un SKU unique pour: " + base);
    }

    public static String normalizePart(String value, int maxLen) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (normalized.length() > maxLen) {
            normalized = normalized.substring(0, maxLen).replaceAll("-+$", "");
        }
        return normalized;
    }

    private static String abbreviateColor(String couleur) {
        String normalized = normalizePart(couleur, 20);
        if (normalized.isEmpty()) {
            return "";
        }
        String abbrev = COLOR_ABBREVIATIONS.get(normalized);
        if (abbrev != null) {
            return abbrev;
        }
        if (normalized.length() <= 4) {
            return normalized;
        }
        return normalized.substring(0, 4);
    }
}
