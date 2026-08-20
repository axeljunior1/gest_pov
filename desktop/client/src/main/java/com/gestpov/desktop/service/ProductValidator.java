package com.gestpov.desktop.service;

import java.math.BigDecimal;

/**
 * Validation UX uniquement. SKU unique, stock et cycle de vie restent côté Spring Boot.
 */
public final class ProductValidator {

    public static final String NAME_REQUIRED = "Le nom est obligatoire.";
    public static final String PRICE_INVALID = "Prix invalide.";
    public static final String PRICE_NEGATIVE = "Le prix ne peut pas être négatif.";
    public static final String PRICE_REQUIRED = "Indiquez le nouveau prix.";

    private ProductValidator() {
    }

    public static String validateName(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            return NAME_REQUIRED;
        }
        return null;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static BigDecimal parsePrice(String raw) {
        String text = normalize(raw).replace(',', '.');
        if (text.isEmpty()) {
            return null;
        }
        return new BigDecimal(text);
    }

    public static String validateOptionalPrice(String raw) {
        String text = normalize(raw);
        if (text.isEmpty()) {
            return null;
        }
        try {
            BigDecimal value = parsePrice(text);
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                return PRICE_NEGATIVE;
            }
        } catch (NumberFormatException e) {
            return PRICE_INVALID;
        }
        return null;
    }

    public static String validateRequiredPrice(String raw) {
        if (normalize(raw).isEmpty()) {
            return PRICE_REQUIRED;
        }
        return validateOptionalPrice(raw);
    }
}
