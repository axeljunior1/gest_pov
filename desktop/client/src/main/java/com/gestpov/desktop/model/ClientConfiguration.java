package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Miroir de ClientConfigurationResponse (entreprise, POS, stock, taxes).
 */
public record ClientConfiguration(
        Company company,
        Pos pos,
        Stock stock,
        Tax tax
) {

    public record Company(
            String name,
            String address,
            String city,
            String country,
            String phone,
            String email,
            String taxId,
            String logoPath,
            String logoUrl,
            String currency,
            String language,
            String timezone,
            String dateFormat
    ) {
    }

    public record Pos(
            String registerName,
            String salePrefix,
            String ticketFormat,
            String ticketFooter,
            boolean ticketShowLogo,
            boolean autoPrintAfterSale,
            boolean changeGivingEnabled,
            boolean allowPartialPayment,
            boolean allowSplitPayment,
            List<PaymentMethodSetting> paymentMethods
    ) {
    }

    public record Stock(
            boolean allowNegativeStock,
            BigDecimal lowStockThresholdDefault,
            String valuationMethod,
            boolean lowStockAlertsEnabled,
            boolean multiWarehouseEnabled
    ) {
    }

    public record Tax(
            boolean enabled,
            String name,
            BigDecimal defaultRate,
            boolean pricesIncludeTax,
            boolean autoApplyOnSales
    ) {
    }

    public record PaymentMethodSetting(String code, String label, boolean enabled) {
        public static PaymentMethodSetting fromJson(JsonNode node) {
            if (node == null || node.isNull()) {
                return null;
            }
            String code = node.path("code").asText("");
            String label = node.path("label").asText("");
            if (label.isBlank()) {
                label = defaultPaymentLabel(code);
            }
            return new PaymentMethodSetting(code, label, node.path("enabled").asBoolean(true));
        }

        private static String defaultPaymentLabel(String code) {
            if (code == null) {
                return "";
            }
            return switch (code.toUpperCase()) {
                case "CASH" -> "Espèces";
                case "CARD" -> "Carte";
                case "MOBILE_MONEY" -> "Mobile money";
                case "BANK_TRANSFER" -> "Virement";
                default -> code;
            };
        }
    }

    public static ClientConfiguration fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return empty();
        }
        JsonNode c = node.path("company");
        JsonNode p = node.path("pos");
        JsonNode s = node.path("stock");
        JsonNode t = node.path("tax");

        List<PaymentMethodSetting> methods = new ArrayList<>();
        JsonNode methodsNode = p.get("paymentMethods");
        if (methodsNode != null && methodsNode.isArray()) {
            methodsNode.forEach(item -> {
                PaymentMethodSetting m = PaymentMethodSetting.fromJson(item);
                if (m != null) {
                    methods.add(m);
                }
            });
        }
        if (methods.isEmpty()) {
            methods.addAll(defaultPaymentMethods());
        }

        return new ClientConfiguration(
                new Company(
                        c.path("name").asText(""),
                        c.path("address").asText(""),
                        c.path("city").asText(""),
                        c.path("country").asText(""),
                        c.path("phone").asText(""),
                        c.path("email").asText(""),
                        c.path("taxId").asText(""),
                        textOrNull(c, "logoPath"),
                        textOrNull(c, "logoUrl"),
                        c.path("currency").asText(""),
                        c.path("language").asText("fr"),
                        c.path("timezone").asText(""),
                        c.path("dateFormat").asText("")
                ),
                new Pos(
                        p.path("registerName").asText(""),
                        p.path("salePrefix").asText(""),
                        p.path("ticketFormat").asText(""),
                        p.path("ticketFooter").asText(""),
                        p.path("ticketShowLogo").asBoolean(true),
                        p.path("autoPrintAfterSale").asBoolean(false),
                        p.path("changeGivingEnabled").asBoolean(true),
                        p.path("allowPartialPayment").asBoolean(false),
                        p.path("allowSplitPayment").asBoolean(true),
                        List.copyOf(methods)
                ),
                new Stock(
                        s.path("allowNegativeStock").asBoolean(false),
                        decimalOr(s, "lowStockThresholdDefault", BigDecimal.TEN),
                        s.path("valuationMethod").asText("WEIGHTED_AVERAGE"),
                        s.path("lowStockAlertsEnabled").asBoolean(true),
                        s.path("multiWarehouseEnabled").asBoolean(true)
                ),
                new Tax(
                        t.path("enabled").asBoolean(false),
                        t.path("name").asText("TVA"),
                        decimalOr(t, "defaultRate", BigDecimal.ZERO),
                        t.path("pricesIncludeTax").asBoolean(true),
                        t.path("autoApplyOnSales").asBoolean(true)
                )
        );
    }

    public static ClientConfiguration empty() {
        return new ClientConfiguration(
                new Company("", "", "", "", "", "", "", null, null, "", "fr", "", ""),
                new Pos("Caisse 1", "TK", "SIMPLE", "", true, false, true, false, true, defaultPaymentMethods()),
                new Stock(false, BigDecimal.TEN, "WEIGHTED_AVERAGE", true, true),
                new Tax(false, "TVA", BigDecimal.ZERO, true, true)
        );
    }

    /** Compatibilité anciens appels SettingsView logo. */
    public String companyName() {
        return company == null ? "" : nullToEmpty(company.name());
    }

    public String logoPath() {
        return company == null ? null : company.logoPath();
    }

    public String logoUrl() {
        return company == null ? null : company.logoUrl();
    }

    public String registerName() {
        return pos == null ? "" : nullToEmpty(pos.registerName());
    }

    public String ticketFooter() {
        return pos == null ? "" : nullToEmpty(pos.ticketFooter());
    }

    public boolean ticketShowLogo() {
        return pos != null && pos.ticketShowLogo();
    }

    public boolean allowPartialPayment() {
        return pos != null && pos.allowPartialPayment();
    }

    public boolean allowSplitPayment() {
        return pos != null && pos.allowSplitPayment();
    }

    public boolean changeGivingEnabled() {
        return pos == null || pos.changeGivingEnabled();
    }

    public List<PaymentMethodSetting> paymentMethods() {
        return pos == null || pos.paymentMethods() == null ? List.of() : pos.paymentMethods();
    }

    private static List<PaymentMethodSetting> defaultPaymentMethods() {
        return List.of(
                new PaymentMethodSetting("CASH", "Espèces", true),
                new PaymentMethodSetting("CARD", "Carte", true),
                new PaymentMethodSetting("MOBILE_MONEY", "Mobile money", true),
                new PaymentMethodSetting("BANK_TRANSFER", "Virement", false)
        );
    }

    private static String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static BigDecimal decimalOr(JsonNode node, String field, BigDecimal fallback) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return fallback;
        }
        JsonNode v = node.get(field);
        try {
            if (v.isNumber()) {
                return v.decimalValue();
            }
            String raw = v.asText(null);
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
