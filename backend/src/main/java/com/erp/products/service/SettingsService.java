package com.erp.products.service;

import com.erp.products.domain.entity.AppSetting;
import com.erp.products.domain.enums.AppSettingType;
import com.erp.products.domain.enums.ReferenceValueCategory;
import com.erp.products.domain.enums.StockValuationMethod;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.AppSettingRepository;
import com.erp.products.settings.SettingKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final Map<String, DefaultSetting> DEFAULTS = buildDefaults();
    private static final Map<String, ReferenceValueCategory> REFERENCE_CATEGORIES = buildReferenceCategories();
    private static final String DEFAULT_LOYALTY_TIERS = """
            [{"name":"BRONZE","minPoints":0,"discountPercent":0},\
            {"name":"SILVER","minPoints":1000,"discountPercent":2},\
            {"name":"GOLD","minPoints":5000,"discountPercent":5},\
            {"name":"PLATINUM","minPoints":10000,"discountPercent":10}]\
            """;

    private final AppSettingRepository settingRepository;
    private final ReferenceValueService referenceValueService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public String getSetting(String key) {
        return settingRepository.findByKey(key)
                .map(AppSetting::getValue)
                .orElseGet(() -> defaultValue(key));
    }

    @Transactional
    public AppSettingResponse setSetting(String key, String value, String updatedBy) {
        DefaultSetting def = DEFAULTS.get(key);
        if (def == null) {
            throw new BusinessException("Cle de parametre inconnue: " + key);
        }
        validateValue(key, def.type(), value);

        AppSetting setting = settingRepository.findByKey(key).orElseGet(() -> AppSetting.builder()
                .key(key)
                .type(def.type())
                .description(def.description())
                .isPublic(def.isPublic())
                .build());
        setting.setValue(value);
        setting.setUpdatedBy(updatedBy);
        setting.setUpdatedAt(Instant.now());
        return toResponse(settingRepository.save(setting));
    }

    @Transactional
    public List<AppSettingResponse> updateSettings(Map<String, String> values, String updatedBy) {
        return values.entrySet().stream()
                .map(e -> setSetting(e.getKey(), e.getValue(), updatedBy))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppSettingResponse> getAllSettings() {
        Map<String, AppSetting> stored = new LinkedHashMap<>();
        settingRepository.findAll().forEach(s -> stored.put(s.getKey(), s));

        return DEFAULTS.keySet().stream()
                .map(key -> stored.containsKey(key)
                        ? toResponse(stored.get(key))
                        : toDefaultResponse(key))
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicSettingsResponse getPublicSettings() {
        return PublicSettingsResponse.builder()
                .companyName(getSetting(SettingKeys.COMPANY_NAME))
                .companyLogo(getSetting(SettingKeys.COMPANY_LOGO))
                .currency(getSetting(SettingKeys.APP_CURRENCY))
                .language(getSetting(SettingKeys.APP_LANGUAGE))
                .timezone(getSetting(SettingKeys.APP_TIMEZONE))
                .dateFormat(getSetting(SettingKeys.APP_DATE_FORMAT))
                .build();
    }

    @Transactional(readOnly = true)
    public NumberingConfigResponse getNumberingConfig() {
        return NumberingConfigResponse.builder()
                .entryPrefix(getSetting(SettingKeys.NUMBERING_ENTRY_PREFIX))
                .exitPrefix(getSetting(SettingKeys.NUMBERING_EXIT_PREFIX))
                .inventoryPrefix(getSetting(SettingKeys.NUMBERING_INVENTORY_PREFIX))
                .movementPrefix(getSetting(SettingKeys.NUMBERING_MOVEMENT_PREFIX))
                .salePrefix(getSetting(SettingKeys.NUMBERING_SALE_PREFIX))
                .build();
    }

    @Transactional(readOnly = true)
    public StockConfigResponse getStockConfig() {
        return StockConfigResponse.builder()
                .allowNegativeStock(getBoolean(SettingKeys.STOCK_ALLOW_NEGATIVE))
                .lowStockThresholdDefault(getDecimal(SettingKeys.STOCK_LOW_THRESHOLD))
                .valuationMethod(parseStockValuationMethod(getSetting(SettingKeys.STOCK_VALUATION_METHOD)))
                .build();
    }

    private StockValuationMethod parseStockValuationMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            return StockValuationMethod.PURCHASE_COST;
        }
        try {
            return StockValuationMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return StockValuationMethod.PURCHASE_COST;
        }
    }

    @Transactional(readOnly = true)
    public AlertConfigResponse getAlertConfig() {
        return AlertConfigResponse.builder()
                .minStockLevelDefault(getDecimal(SettingKeys.STOCK_LOW_THRESHOLD))
                .expiryAlertDaysDefault(getInteger(SettingKeys.ALERT_EXPIRY_DAYS))
                .build();
    }

    @Transactional(readOnly = true)
    public LoyaltyConfigResponse getLoyaltyConfig() {
        return LoyaltyConfigResponse.builder()
                .loyaltyEnabled(getBoolean(SettingKeys.LOYALTY_ENABLED))
                .pointsPerCurrencyUnit(getDecimal(SettingKeys.LOYALTY_POINTS_PER_CURRENCY_UNIT))
                .currencyUnitAmount(getDecimal(SettingKeys.LOYALTY_CURRENCY_UNIT_AMOUNT))
                .pointValue(getDecimal(SettingKeys.LOYALTY_POINT_VALUE))
                .minimumPointsToRedeem(getInteger(SettingKeys.LOYALTY_MINIMUM_POINTS_TO_REDEEM))
                .maximumDiscountPercent(getDecimal(SettingKeys.LOYALTY_MAXIMUM_DISCOUNT_PERCENT))
                .pointsExpirationEnabled(getBoolean(SettingKeys.LOYALTY_POINTS_EXPIRATION_ENABLED))
                .pointsExpirationDays(getInteger(SettingKeys.LOYALTY_POINTS_EXPIRATION_DAYS))
                .earnPointsOnDiscountedSales(getBoolean(SettingKeys.LOYALTY_EARN_ON_DISCOUNTED_SALES))
                .earnPointsOnTaxIncludedAmount(getBoolean(SettingKeys.LOYALTY_EARN_ON_TAX_INCLUDED))
                .allowPointsRedemption(getBoolean(SettingKeys.LOYALTY_ALLOW_REDEMPTION))
                .loyaltyTiersConfig(parseLoyaltyTiers(getSetting(SettingKeys.LOYALTY_TIERS_CONFIG)))
                .build();
    }

    public List<LoyaltyTierConfig> parseLoyaltyTiers(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new BusinessException("Configuration niveaux fidélité invalide");
        }
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(getSetting(key));
    }

    public BigDecimal getDecimal(String key) {
        String value = getSetting(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.replace(",", "."));
    }

    public Integer getInteger(String key) {
        String value = getSetting(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    @Transactional(readOnly = true)
    public PosConfigResponse getPosConfig() {
        Integer maxPending = getInteger(SettingKeys.POS_MAX_PENDING_PAYMENT_DURATION);
        Integer alertPending = getInteger(SettingKeys.POS_ALERT_PENDING_PAYMENT_MINUTES);
        Integer alertCash = getInteger(SettingKeys.POS_ALERT_CASH_DIFFERENCE_THRESHOLD);
        String salesFlowMode = resolveSalesFlowMode();
        return PosConfigResponse.builder()
                .salesFlowMode(salesFlowMode)
                .cashHandlingMode(mapSalesFlowToLegacyMode(salesFlowMode))
                .allowSellerCashCollection(getBoolean(SettingKeys.POS_ALLOW_SELLER_CASH_COLLECTION))
                .allowPartialPayment(getBoolean(SettingKeys.POS_ALLOW_PARTIAL_PAYMENT))
                .allowSplitPayment(getBoolean(SettingKeys.POS_ALLOW_SPLIT_PAYMENT))
                .maxPendingPaymentDurationMinutes(maxPending != null ? maxPending : 120)
                .alertPendingPaymentMinutes(alertPending != null ? alertPending : 15)
                .alertCashDifferenceThreshold(alertCash != null ? alertCash : 20)
                .requireManagerValidationForCashDifference(
                        getBoolean(SettingKeys.POS_REQUIRE_MANAGER_VALIDATION_FOR_CASH_DIFFERENCE))
                .build();
    }

    @Transactional(readOnly = true)
    public BarcodeScanConfig getBarcodeScanConfig() {
        Integer minLength = getInteger(SettingKeys.POS_BARCODE_MIN_LENGTH);
        return BarcodeScanConfig.builder()
                .scanEnabled(getBoolean(SettingKeys.POS_BARCODE_SCAN_ENABLED))
                .minLength(minLength != null ? minLength : 6)
                .autoAddToCart(getBoolean(SettingKeys.POS_BARCODE_AUTO_ADD_TO_CART))
                .searchPriority(getSetting(SettingKeys.POS_BARCODE_SEARCH_PRIORITY))
                .build();
    }

    private String resolveSalesFlowMode() {
        String mode = getSetting(SettingKeys.POS_SALES_FLOW_MODE);
        if (mode != null && !mode.isBlank()) {
            return mode.trim();
        }
        String legacy = getSetting(SettingKeys.POS_CASH_HANDLING_MODE);
        if ("CENTRAL_CASHIER".equals(legacy)) {
            return "CENTRAL_CASHIER";
        }
        if ("SELLER_CASHIER".equals(legacy)) {
            return "SELLER_COLLECTS_PAYMENT";
        }
        return "CENTRAL_CASHIER";
    }

    private static String mapSalesFlowToLegacyMode(String salesFlowMode) {
        if ("CENTRAL_CASHIER".equals(salesFlowMode)) {
            return "CENTRAL_CASHIER";
        }
        return "SELLER_CASHIER";
    }

    public void ensureDefaultsSeeded() {
        for (Map.Entry<String, DefaultSetting> entry : DEFAULTS.entrySet()) {
            if (settingRepository.findByKey(entry.getKey()).isEmpty()) {
                DefaultSetting def = entry.getValue();
                settingRepository.save(AppSetting.builder()
                        .key(entry.getKey())
                        .value(def.value())
                        .type(def.type())
                        .description(def.description())
                        .isPublic(def.isPublic())
                        .updatedBy("system")
                        .updatedAt(Instant.now())
                        .build());
            }
        }
    }

    private AppSettingResponse toResponse(AppSetting setting) {
        return AppSettingResponse.builder()
                .id(setting.getId())
                .key(setting.getKey())
                .value(setting.getValue())
                .type(setting.getType())
                .description(setting.getDescription())
                .isPublic(setting.getIsPublic())
                .referenceCategory(REFERENCE_CATEGORIES.get(setting.getKey()))
                .updatedBy(setting.getUpdatedBy())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    private AppSettingResponse toDefaultResponse(String key) {
        DefaultSetting def = DEFAULTS.get(key);
        return AppSettingResponse.builder()
                .key(key)
                .value(def.value())
                .type(def.type())
                .description(def.description())
                .isPublic(def.isPublic())
                .referenceCategory(REFERENCE_CATEGORIES.get(key))
                .build();
    }

    private static String defaultValue(String key) {
        DefaultSetting def = DEFAULTS.get(key);
        if (def == null) {
            throw new ResourceNotFoundException("Parametre inconnu: " + key);
        }
        return def.value();
    }

    private void validateValue(String key, AppSettingType type, String value) {
        if (value == null) {
            throw new BusinessException("Valeur obligatoire");
        }
        switch (type) {
            case BOOLEAN -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new BusinessException("Valeur booleenne invalide");
                }
            }
            case NUMBER -> {
                try {
                    new BigDecimal(value.replace(",", "."));
                } catch (NumberFormatException e) {
                    throw new BusinessException("Valeur numerique invalide");
                }
            }
            case STRING, JSON -> { /* ok */ }
        }
        ReferenceValueCategory category = REFERENCE_CATEGORIES.get(key);
        if (category != null) {
            referenceValueService.requireValid(category, value);
        }
    }

    private static Map<String, ReferenceValueCategory> buildReferenceCategories() {
        Map<String, ReferenceValueCategory> map = new LinkedHashMap<>();
        map.put(SettingKeys.APP_CURRENCY, ReferenceValueCategory.CURRENCY);
        map.put(SettingKeys.APP_LANGUAGE, ReferenceValueCategory.LANGUAGE);
        map.put(SettingKeys.APP_TIMEZONE, ReferenceValueCategory.TIMEZONE);
        map.put(SettingKeys.APP_DATE_FORMAT, ReferenceValueCategory.DATE_FORMAT);
        map.put(SettingKeys.POS_SALES_FLOW_MODE, ReferenceValueCategory.POS_SALES_FLOW_MODE);
        map.put(SettingKeys.STOCK_VALUATION_METHOD, ReferenceValueCategory.STOCK_VALUATION_METHOD);
        return map;
    }

    private static Map<String, DefaultSetting> buildDefaults() {
        Map<String, DefaultSetting> map = new LinkedHashMap<>();
        map.put(SettingKeys.COMPANY_NAME, def("ERP Produits", AppSettingType.STRING, "Nom de l'entreprise", true));
        map.put(SettingKeys.COMPANY_LOGO, def("", AppSettingType.STRING, "URL ou chemin du logo", true));
        map.put(SettingKeys.APP_CURRENCY, def("EUR", AppSettingType.STRING, "Devise par defaut", true));
        map.put(SettingKeys.APP_LANGUAGE, def("fr", AppSettingType.STRING, "Langue par defaut", true));
        map.put(SettingKeys.APP_TIMEZONE, def("Europe/Paris", AppSettingType.STRING, "Fuseau horaire", true));
        map.put(SettingKeys.APP_DATE_FORMAT, def("dd/MM/yyyy", AppSettingType.STRING, "Format de date", true));
        map.put(SettingKeys.STOCK_ALLOW_NEGATIVE, def("false", AppSettingType.BOOLEAN, "Autoriser le stock negatif", false));
        map.put(SettingKeys.STOCK_LOW_THRESHOLD, def("10", AppSettingType.NUMBER, "Seuil stock faible par defaut", false));
        map.put(SettingKeys.STOCK_VALUATION_METHOD, def("PURCHASE_COST", AppSettingType.STRING,
                "Methode de valorisation du stock (PURCHASE_COST ou SALE_PRICE)", false));
        map.put(SettingKeys.ALERT_EXPIRY_DAYS, def("30", AppSettingType.NUMBER, "Delai alerte peremption (jours)", false));
        map.put(SettingKeys.NUMBERING_ENTRY_PREFIX, def("SE", AppSettingType.STRING, "Prefixe numerotation entrees", false));
        map.put(SettingKeys.NUMBERING_EXIT_PREFIX, def("SX", AppSettingType.STRING, "Prefixe numerotation sorties", false));
        map.put(SettingKeys.NUMBERING_INVENTORY_PREFIX, def("INV", AppSettingType.STRING, "Prefixe numerotation inventaires", false));
        map.put(SettingKeys.NUMBERING_MOVEMENT_PREFIX, def("MV", AppSettingType.STRING, "Prefixe numerotation mouvements", false));
        map.put(SettingKeys.NUMBERING_SALE_PREFIX, def("TK", AppSettingType.STRING, "Prefixe numerotation ventes POS", false));
        map.put(SettingKeys.POS_REGISTER_NAME, def("Caisse 1", AppSettingType.STRING, "Nom de la caisse POS", true));
        map.put(SettingKeys.POS_TAX_RATE_DEFAULT, def("0", AppSettingType.NUMBER, "Taux TVA par defaut (%)", false));
        map.put(SettingKeys.POS_DEFAULT_WAREHOUSE_CODE, def("WH-MAIN", AppSettingType.STRING, "Code entrepot POS par defaut", false));
        map.put(SettingKeys.POS_SALES_FLOW_MODE, def("CENTRAL_CASHIER", AppSettingType.STRING,
                "Mode flux POS: SELLER_COLLECTS_PAYMENT ou CENTRAL_CASHIER", false));
        map.put(SettingKeys.POS_CASH_HANDLING_MODE, def("CENTRAL_CASHIER", AppSettingType.STRING,
                "Deprecated — utiliser pos_sales_flow_mode", false));
        map.put(SettingKeys.POS_ALLOW_SELLER_CASH_COLLECTION, def("false", AppSettingType.BOOLEAN, "Autoriser encaissement vendeur", false));
        map.put(SettingKeys.POS_ALLOW_PARTIAL_PAYMENT, def("false", AppSettingType.BOOLEAN, "Autoriser paiement partiel", false));
        map.put(SettingKeys.POS_ALLOW_SPLIT_PAYMENT, def("true", AppSettingType.BOOLEAN, "Autoriser paiement fractionne", false));
        map.put(SettingKeys.POS_MAX_PENDING_PAYMENT_DURATION, def("120", AppSettingType.NUMBER, "Duree max attente paiement (minutes)", false));
        map.put(SettingKeys.POS_ALERT_PENDING_PAYMENT_MINUTES, def("15", AppSettingType.NUMBER, "Alerte vente en attente (minutes)", false));
        map.put(SettingKeys.POS_ALERT_CASH_DIFFERENCE_THRESHOLD, def("20", AppSettingType.NUMBER, "Seuil alerte ecart caisse", false));
        map.put(SettingKeys.POS_REQUIRE_MANAGER_VALIDATION_FOR_CASH_DIFFERENCE, def("false", AppSettingType.BOOLEAN,
                "Validation manager obligatoire si ecart caisse a la cloture", false));
        map.put(SettingKeys.POS_REQUIRE_MANAGER_APPROVAL_ABOVE_REFUND_AMOUNT, def("999999", AppSettingType.NUMBER,
                "Seuil remboursement necessitant validation manager", false));
        map.put(SettingKeys.POS_BARCODE_SCAN_ENABLED, def("true", AppSettingType.BOOLEAN,
                "Activer le scan code-barres POS", true));
        map.put(SettingKeys.POS_BARCODE_MIN_LENGTH, def("6", AppSettingType.NUMBER,
                "Longueur minimale pour detection scan code-barres", true));
        map.put(SettingKeys.POS_BARCODE_AUTO_ADD_TO_CART, def("true", AppSettingType.BOOLEAN,
                "Ajout automatique au panier apres scan", true));
        map.put(SettingKeys.POS_BARCODE_SEARCH_PRIORITY, def("packaging,variant,product", AppSettingType.STRING,
                "Priorite recherche code-barres (packaging,variant,product)", false));
        map.put(SettingKeys.LOYALTY_ENABLED, def("true", AppSettingType.BOOLEAN, "Activer la fidelite", false));
        map.put(SettingKeys.LOYALTY_POINTS_PER_CURRENCY_UNIT, def("1", AppSettingType.NUMBER, "Points gagnes par unite de devise", false));
        map.put(SettingKeys.LOYALTY_CURRENCY_UNIT_AMOUNT, def("1", AppSettingType.NUMBER, "Montant devise pour le calcul des points", false));
        map.put(SettingKeys.LOYALTY_POINT_VALUE, def("0.05", AppSettingType.NUMBER, "Valeur monetaire d un point", false));
        map.put(SettingKeys.LOYALTY_MINIMUM_POINTS_TO_REDEEM, def("100", AppSettingType.NUMBER, "Minimum de points pour utilisation", false));
        map.put(SettingKeys.LOYALTY_MAXIMUM_DISCOUNT_PERCENT, def("50", AppSettingType.NUMBER, "Remise max fidélité (% du total)", false));
        map.put(SettingKeys.LOYALTY_POINTS_EXPIRATION_ENABLED, def("false", AppSettingType.BOOLEAN, "Expiration des points activee", false));
        map.put(SettingKeys.LOYALTY_POINTS_EXPIRATION_DAYS, def("365", AppSettingType.NUMBER, "Duree validite points (jours)", false));
        map.put(SettingKeys.LOYALTY_EARN_ON_DISCOUNTED_SALES, def("true", AppSettingType.BOOLEAN, "Points sur ventes remisees", false));
        map.put(SettingKeys.LOYALTY_EARN_ON_TAX_INCLUDED, def("false", AppSettingType.BOOLEAN, "Calcul points TTC", false));
        map.put(SettingKeys.LOYALTY_ALLOW_REDEMPTION, def("true", AppSettingType.BOOLEAN, "Autoriser utilisation points", false));
        map.put(SettingKeys.LOYALTY_TIERS_CONFIG, def(DEFAULT_LOYALTY_TIERS, AppSettingType.JSON, "Niveaux fidelite (JSON)", false));
        return map;
    }

    private static DefaultSetting def(String value, AppSettingType type, String description, boolean isPublic) {
        return new DefaultSetting(value, type, description, isPublic);
    }

    private record DefaultSetting(String value, AppSettingType type, String description, boolean isPublic) {}
}
