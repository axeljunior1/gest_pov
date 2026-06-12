package com.erp.products.service.alert;

import com.erp.products.domain.entity.AlertSetting;
import com.erp.products.domain.enums.AlertSettingScope;
import com.erp.products.dto.AlertConfigResponse;
import com.erp.products.repository.AlertSettingRepository;
import com.erp.products.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AlertSettingResolver {

    private static final BigDecimal DEFAULT_MAX = new BigDecimal("1000");
    private static final int DEFAULT_DORMANT_DAYS = 90;

    private final AlertSettingRepository settingRepository;
    private final SettingsService settingsService;

    @Transactional(readOnly = true)
    public EffectiveAlertSettings resolve(Long productId, Long warehouseId) {
        MutableSettings settings = new MutableSettings();

        settingRepository.findByScopeAndActifTrue(AlertSettingScope.GLOBAL)
                .ifPresent(s -> apply(settings, s));

        if (warehouseId != null) {
            settingRepository.findByScopeAndWarehouseIdAndActifTrue(AlertSettingScope.WAREHOUSE, warehouseId)
                    .ifPresent(s -> apply(settings, s));
        }
        if (productId != null) {
            settingRepository.findByScopeAndProductIdAndActifTrue(AlertSettingScope.PRODUCT, productId)
                    .ifPresent(s -> apply(settings, s));
        }
        if (productId != null && warehouseId != null) {
            settingRepository.findProductWarehouseSetting(productId, warehouseId)
                    .ifPresent(s -> apply(settings, s));
        }

        return settings.toEffective(settingsService.getAlertConfig());
    }

    private void apply(MutableSettings settings, AlertSetting s) {
        if (s.getMinStockLevel() != null) {
            settings.minStockLevel = s.getMinStockLevel();
        }
        if (s.getMaxStockLevel() != null) {
            settings.maxStockLevel = s.getMaxStockLevel();
        }
        if (s.getExpiryAlertDays() != null) {
            settings.expiryAlertDays = s.getExpiryAlertDays();
        }
        if (s.getDormantDays() != null) {
            settings.dormantDays = s.getDormantDays();
        }
    }

    public record EffectiveAlertSettings(
            BigDecimal minStockLevel,
            BigDecimal maxStockLevel,
            Integer expiryAlertDays,
            Integer dormantDays) {}

    private static final class MutableSettings {
        BigDecimal minStockLevel;
        BigDecimal maxStockLevel;
        Integer expiryAlertDays;
        Integer dormantDays;

        EffectiveAlertSettings toEffective(AlertConfigResponse defaults) {
            return new EffectiveAlertSettings(
                    minStockLevel != null ? minStockLevel : defaults.getMinStockLevelDefault(),
                    maxStockLevel != null ? maxStockLevel : DEFAULT_MAX,
                    expiryAlertDays != null ? expiryAlertDays : defaults.getExpiryAlertDaysDefault(),
                    dormantDays != null ? dormantDays : DEFAULT_DORMANT_DAYS);
        }
    }
}
