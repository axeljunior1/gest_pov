package com.gestpov.desktop.service;

/**
 * Validation UX uniquement. Enfants, produits rattachés et parent invalide restent côté Spring Boot.
 */
public final class CategoryValidator {

    public static final String NAME_REQUIRED = "Le nom de la catégorie est obligatoire.";

    private CategoryValidator() {
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
