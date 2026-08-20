package com.gestpov.desktop.ui.products;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ProductLabels {

    static final String[] STATUTS = {"ACTIF", "INACTIF", "ARCHIVE"};
    static final String[] CYCLES = {
            "BROUILLON", "EN_ATTENTE_VALIDATION", "ACTIF", "SUSPENDU", "ARRETE", "ARCHIVE"
    };
    static final String[] PRICE_TYPES = {"ACHAT", "VENTE", "PROMOTIONNEL"};

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ProductLabels() {
    }

    static String status(String code) {
        if (code == null) {
            return "—";
        }
        return switch (code) {
            case "ACTIF" -> "Actif";
            case "INACTIF" -> "Inactif";
            case "ARCHIVE" -> "Archivé";
            default -> code;
        };
    }

    static String lifecycle(String code) {
        if (code == null) {
            return "—";
        }
        return switch (code) {
            case "BROUILLON" -> "Brouillon";
            case "EN_ATTENTE_VALIDATION" -> "En attente validation";
            case "ACTIF" -> "Actif";
            case "SUSPENDU" -> "Suspendu";
            case "ARRETE" -> "Arrêté";
            case "ARCHIVE" -> "Archivé";
            default -> code;
        };
    }

    public static String price(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " €";
    }

    static String date(String iso) {
        if (iso == null || iso.isBlank()) {
            return "—";
        }
        try {
            Instant instant = Instant.parse(iso);
            LocalDate local = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            return DATE.format(local);
        } catch (Exception e) {
            return iso.length() >= 10 ? iso.substring(0, 10) : iso;
        }
    }
}
