package com.gestpov.desktop.ui.products;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ProductLabels {

    static final String[] STATUTS = {"ACTIF", "INACTIF", "ARCHIVE"};
    static final String[] CYCLES = {
            "BROUILLON", "EN_ATTENTE_VALIDATION", "ACTIF", "SUSPENDU", "ARRETE", "ARCHIVE"
    };
    static final String[] PRICE_TYPES = {"ACHAT", "VENTE", "PROMOTIONNEL"};

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Devise affichee, alimentee une fois au demarrage depuis ClientConfiguration.company().currency(). */
    private static volatile String currencyCode = "XAF";

    private ProductLabels() {
    }

    /** A appeler une seule fois (post-login) avec la devise configuree cote serveur. */
    public static void setCurrency(String isoCode) {
        if (isoCode != null && !isoCode.isBlank()) {
            currencyCode = isoCode.trim().toUpperCase(Locale.ROOT);
        }
    }

    public static String currencyCode() {
        return currencyCode;
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
        int fractionDigits = 2;
        String symbol = currencyCode;
        try {
            java.util.Currency currency = java.util.Currency.getInstance(currencyCode);
            fractionDigits = Math.max(currency.getDefaultFractionDigits(), 0);
            symbol = currency.getSymbol(Locale.FRANCE);
        } catch (Exception ignored) {
            // code devise inconnu du JDK : on affiche le code brut tel quel, 2 decimales par defaut
        }
        return value.setScale(fractionDigits, RoundingMode.HALF_UP).toPlainString() + " " + symbol;
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
