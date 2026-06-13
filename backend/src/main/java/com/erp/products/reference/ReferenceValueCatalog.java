package com.erp.products.reference;

import com.erp.products.domain.enums.ReferenceValueCategory;

import java.util.List;

public final class ReferenceValueCatalog {

    private ReferenceValueCatalog() {}

    public record Seed(ReferenceValueCategory category, String code, String label, int sortOrder) {}

    public static List<Seed> all() {
        return List.of(
                // Devises
                seed(ReferenceValueCategory.CURRENCY, "EUR", "Euro (EUR)", 1),
                seed(ReferenceValueCategory.CURRENCY, "USD", "Dollar US (USD)", 2),
                seed(ReferenceValueCategory.CURRENCY, "GBP", "Livre sterling (GBP)", 3),
                seed(ReferenceValueCategory.CURRENCY, "CHF", "Franc suisse (CHF)", 4),
                seed(ReferenceValueCategory.CURRENCY, "XOF", "Franc CFA UEMOA (XOF)", 5),
                seed(ReferenceValueCategory.CURRENCY, "XAF", "Franc CFA CEMAC (XAF)", 6),
                seed(ReferenceValueCategory.CURRENCY, "MAD", "Dirham marocain (MAD)", 7),
                seed(ReferenceValueCategory.CURRENCY, "TND", "Dinar tunisien (TND)", 8),
                seed(ReferenceValueCategory.CURRENCY, "DZD", "Dinar algérien (DZD)", 9),
                seed(ReferenceValueCategory.CURRENCY, "CAD", "Dollar canadien (CAD)", 10),

                // Langues
                seed(ReferenceValueCategory.LANGUAGE, "fr", "Français", 1),
                seed(ReferenceValueCategory.LANGUAGE, "en", "English", 2),
                seed(ReferenceValueCategory.LANGUAGE, "es", "Español", 3),
                seed(ReferenceValueCategory.LANGUAGE, "de", "Deutsch", 4),
                seed(ReferenceValueCategory.LANGUAGE, "pt", "Português", 5),
                seed(ReferenceValueCategory.LANGUAGE, "ar", "العربية", 6),

                // Fuseaux horaires
                seed(ReferenceValueCategory.TIMEZONE, "Europe/Paris", "Europe/Paris (France)", 1),
                seed(ReferenceValueCategory.TIMEZONE, "Europe/London", "Europe/London (UK)", 2),
                seed(ReferenceValueCategory.TIMEZONE, "Africa/Abidjan", "Africa/Abidjan (UEMOA)", 3),
                seed(ReferenceValueCategory.TIMEZONE, "Africa/Dakar", "Africa/Dakar (Sénégal)", 4),
                seed(ReferenceValueCategory.TIMEZONE, "Africa/Casablanca", "Africa/Casablanca (Maroc)", 5),
                seed(ReferenceValueCategory.TIMEZONE, "Africa/Lagos", "Africa/Lagos (Nigeria)", 6),
                seed(ReferenceValueCategory.TIMEZONE, "Africa/Douala", "Africa/Douala (Cameroun)", 7),
                seed(ReferenceValueCategory.TIMEZONE, "UTC", "UTC", 8),

                // Formats de date
                seed(ReferenceValueCategory.DATE_FORMAT, "dd/MM/yyyy", "jj/mm/aaaa (dd/MM/yyyy)", 1),
                seed(ReferenceValueCategory.DATE_FORMAT, "MM/dd/yyyy", "mm/jj/aaaa (MM/dd/yyyy)", 2),
                seed(ReferenceValueCategory.DATE_FORMAT, "yyyy-MM-dd", "aaaa-mm-jj (yyyy-MM-dd)", 3),
                seed(ReferenceValueCategory.DATE_FORMAT, "dd-MM-yyyy", "jj-mm-aaaa (dd-MM-yyyy)", 4),

                // Flux POS
                seed(ReferenceValueCategory.POS_SALES_FLOW_MODE, "SELLER_COLLECTS_PAYMENT",
                        "Vendeur encaisseur", 1),
                seed(ReferenceValueCategory.POS_SALES_FLOW_MODE, "CENTRAL_CASHIER",
                        "Caisse centrale", 2)
        );
    }

    private static Seed seed(ReferenceValueCategory category, String code, String label, int sortOrder) {
        return new Seed(category, code, label, sortOrder);
    }
}
