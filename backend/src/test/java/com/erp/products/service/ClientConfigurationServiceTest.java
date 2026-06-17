package com.erp.products.service;

import com.erp.products.settings.SettingKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClientConfigurationServiceTest {

    @Autowired
    private ClientConfigurationService clientConfigurationService;

    @Autowired
    private SettingsService settingsService;

    @Test
    void resolveSaleLineTaxRateRespectsTaxSettings() {
        settingsService.setSetting(SettingKeys.TAX_ENABLED, "true", "test");
        settingsService.setSetting(SettingKeys.TAX_AUTO_APPLY_ON_SALES, "true", "test");
        settingsService.setSetting(SettingKeys.POS_TAX_RATE_DEFAULT, "18", "test");

        assertThat(clientConfigurationService.resolveSaleLineTaxRate(null))
                .isEqualByComparingTo(new BigDecimal("18"));
    }

    @Test
    void resolveSaleLineTaxRateReturnsZeroWhenTaxDisabled() {
        settingsService.setSetting(SettingKeys.TAX_ENABLED, "false", "test");
        settingsService.setSetting(SettingKeys.POS_TAX_RATE_DEFAULT, "18", "test");

        assertThat(clientConfigurationService.resolveSaleLineTaxRate(null))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void defaultPaymentMethodsIncludeCash() {
        assertThat(clientConfigurationService.parsePaymentMethods())
                .anyMatch(m -> "CASH".equals(m.getCode()) && m.isEnabled());
    }
}
