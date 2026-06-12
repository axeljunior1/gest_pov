package com.erp.products.service.alert;

import com.erp.products.domain.entity.AlertSetting;
import com.erp.products.domain.enums.AlertSettingScope;
import com.erp.products.repository.AlertSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AlertSettingResolver {

    private static final BigDecimal DEFAULT_MIN = BigDecimal.TEN;
    private static final BigDecimal DEFAULT_MAX = new BigDecimal("1000");
    private static final int DEFAULT_EXPIRY_DAYS = 30;
    private static final int DEFAULT_DORMANT_DAYS = 90;

    private final AlertSettingRepository settingRepository;

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

        return settings.toEffective();
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

        EffectiveAlertSettings toEffective() {
            return new EffectiveAlertSettings(
                    minStockLevel != null ? minStockLevel : DEFAULT_MIN,
                    maxStockLevel != null ? maxStockLevel : DEFAULT_MAX,
                    expiryAlertDays != null ? expiryAlertDays : DEFAULT_EXPIRY_DAYS,
                    dormantDays != null ? dormantDays : DEFAULT_DORMANT_DAYS);
        }
    }
}
