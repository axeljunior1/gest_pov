package com.erp.products.service;

import com.erp.products.domain.entity.AppSetting;
import com.erp.products.domain.enums.AppSettingType;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.AppSettingRepository;
import com.erp.products.settings.SettingKeys;
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

    private final AppSettingRepository settingRepository;

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
        validateValue(def.type(), value);

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
                .build();
    }

    @Transactional(readOnly = true)
    public AlertConfigResponse getAlertConfig() {
        return AlertConfigResponse.builder()
                .minStockLevelDefault(getDecimal(SettingKeys.STOCK_LOW_THRESHOLD))
                .expiryAlertDaysDefault(getInteger(SettingKeys.ALERT_EXPIRY_DAYS))
                .build();
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
                .build();
    }

    private static String defaultValue(String key) {
        DefaultSetting def = DEFAULTS.get(key);
        if (def == null) {
            throw new ResourceNotFoundException("Parametre inconnu: " + key);
        }
        return def.value();
    }

    private static void validateValue(AppSettingType type, String value) {
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
        map.put(SettingKeys.ALERT_EXPIRY_DAYS, def("30", AppSettingType.NUMBER, "Delai alerte peremption (jours)", false));
        map.put(SettingKeys.NUMBERING_ENTRY_PREFIX, def("SE", AppSettingType.STRING, "Prefixe numerotation entrees", false));
        map.put(SettingKeys.NUMBERING_EXIT_PREFIX, def("SX", AppSettingType.STRING, "Prefixe numerotation sorties", false));
        map.put(SettingKeys.NUMBERING_INVENTORY_PREFIX, def("INV", AppSettingType.STRING, "Prefixe numerotation inventaires", false));
        map.put(SettingKeys.NUMBERING_MOVEMENT_PREFIX, def("MV", AppSettingType.STRING, "Prefixe numerotation mouvements", false));
        map.put(SettingKeys.NUMBERING_SALE_PREFIX, def("TK", AppSettingType.STRING, "Prefixe numerotation ventes POS", false));
        map.put(SettingKeys.POS_REGISTER_NAME, def("Caisse 1", AppSettingType.STRING, "Nom de la caisse POS", true));
        map.put(SettingKeys.POS_TAX_RATE_DEFAULT, def("0", AppSettingType.NUMBER, "Taux TVA par defaut (%)", false));
        map.put(SettingKeys.POS_DEFAULT_WAREHOUSE_CODE, def("WH-MAIN", AppSettingType.STRING, "Code entrepot POS par defaut", false));
        return map;
    }

    private static DefaultSetting def(String value, AppSettingType type, String description, boolean isPublic) {
        return new DefaultSetting(value, type, description, isPublic);
    }

    private record DefaultSetting(String value, AppSettingType type, String description, boolean isPublic) {}
}
