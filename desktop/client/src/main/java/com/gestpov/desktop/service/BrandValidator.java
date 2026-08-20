package com.gestpov.desktop.service;

/**
 * Validation UX uniquement. Unicité et liens produits restent côté Spring Boot.
 */
public final class BrandValidator {

    public static final String NAME_REQUIRED = "Le nom de la marque est obligatoire.";

    private BrandValidator() {
    }

    public static String validateName(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            return NAME_REQUIRED;
        }
        return null;
    }

    public static String normalizeName(String nom) {
        return nom == null ? "" : nom.trim();
    }
}
