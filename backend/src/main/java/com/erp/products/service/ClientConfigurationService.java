package com.erp.products.service;

import com.erp.products.domain.enums.StockValuationMethod;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.settings.SettingKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClientConfigurationService {

    static final String DEFAULT_PAYMENT_METHODS_JSON = """
            [{"code":"CASH","label":"Especes","enabled":true},\
            {"code":"CARD","label":"Carte","enabled":true},\
            {"code":"MOBILE_MONEY","label":"Mobile money","enabled":true},\
            {"code":"BANK_TRANSFER","label":"Virement","enabled":false}]\
            """;

    private final SettingsService settingsService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ClientConfigurationResponse getClientConfiguration() {
        var publicSettings = settingsService.getPublicSettings();
        return ClientConfigurationResponse.builder()
                .company(ClientConfigurationResponse.CompanySection.builder()
                        .name(publicSettings.getCompanyName())
                        .address(publicSettings.getCompanyAddress())
                        .city(publicSettings.getCompanyCity())
                        .country(publicSettings.getCompanyCountry())
                        .phone(publicSettings.getCompanyPhone())
                        .email(publicSettings.getCompanyEmail())
                        .taxId(publicSettings.getCompanyTaxId())
                        .logoPath(settingsService.getSetting(SettingKeys.COMPANY_LOGO))
                        .logoUrl(publicSettings.getCompanyLogoUrl())
                        .currency(publicSettings.getCurrency())
                        .language(publicSettings.getLanguage())
                        .timezone(publicSettings.getTimezone())
                        .dateFormat(publicSettings.getDateFormat())
                        .build())
                .pos(ClientConfigurationResponse.PosSection.builder()
                        .registerName(settingsService.getSetting(SettingKeys.POS_REGISTER_NAME))
                        .salePrefix(settingsService.getSetting(SettingKeys.NUMBERING_SALE_PREFIX))
                        .ticketFormat(settingsService.getSetting(SettingKeys.POS_TICKET_FORMAT))
                        .ticketFooter(settingsService.getSetting(SettingKeys.POS_TICKET_FOOTER))
                        .ticketShowLogo(settingsService.getBoolean(SettingKeys.POS_TICKET_SHOW_LOGO))
                        .autoPrintAfterSale(settingsService.getBoolean(SettingKeys.POS_AUTO_PRINT_AFTER_SALE))
                        .changeGivingEnabled(settingsService.getBoolean(SettingKeys.POS_CHANGE_GIVING_ENABLED))
                        .paymentMethods(parsePaymentMethods())
                        .allowPartialPayment(settingsService.getPosConfig().isAllowPartialPayment())
                        .allowSplitPayment(settingsService.getPosConfig().isAllowSplitPayment())
                        .build())
                .stock(buildStockSection())
                .tax(buildTaxSection())
                .build();
    }

    @Transactional
    public ClientConfigurationResponse updateClientConfiguration(ClientConfigurationUpdateRequest request, String updatedBy) {
        if (request.getCompany() != null) {
            var c = request.getCompany();
            settingsService.setSetting(SettingKeys.COMPANY_NAME, c.getName(), updatedBy);
            setOptional(SettingKeys.COMPANY_ADDRESS, c.getAddress(), updatedBy);
            setOptional(SettingKeys.COMPANY_CITY, c.getCity(), updatedBy);
            setOptional(SettingKeys.COMPANY_COUNTRY, c.getCountry(), updatedBy);
            setOptional(SettingKeys.COMPANY_PHONE, c.getPhone(), updatedBy);
            setOptional(SettingKeys.COMPANY_EMAIL, c.getEmail(), updatedBy);
            setOptional(SettingKeys.COMPANY_TAX_ID, c.getTaxId(), updatedBy);
            if (c.getCurrency() != null) settingsService.setSetting(SettingKeys.APP_CURRENCY, c.getCurrency(), updatedBy);
            if (c.getLanguage() != null) settingsService.setSetting(SettingKeys.APP_LANGUAGE, c.getLanguage(), updatedBy);
            if (c.getTimezone() != null) settingsService.setSetting(SettingKeys.APP_TIMEZONE, c.getTimezone(), updatedBy);
            if (c.getDateFormat() != null) settingsService.setSetting(SettingKeys.APP_DATE_FORMAT, c.getDateFormat(), updatedBy);
        }
        if (request.getPos() != null) {
            var p = request.getPos();
            if (p.getRegisterName() != null) settingsService.setSetting(SettingKeys.POS_REGISTER_NAME, p.getRegisterName(), updatedBy);
            if (p.getSalePrefix() != null) settingsService.setSetting(SettingKeys.NUMBERING_SALE_PREFIX, p.getSalePrefix(), updatedBy);
            if (p.getTicketFormat() != null) settingsService.setSetting(SettingKeys.POS_TICKET_FORMAT, p.getTicketFormat(), updatedBy);
            setOptional(SettingKeys.POS_TICKET_FOOTER, p.getTicketFooter(), updatedBy);
            if (p.getTicketShowLogo() != null) settingsService.setSetting(SettingKeys.POS_TICKET_SHOW_LOGO, bool(p.getTicketShowLogo()), updatedBy);
            if (p.getAutoPrintAfterSale() != null) settingsService.setSetting(SettingKeys.POS_AUTO_PRINT_AFTER_SALE, bool(p.getAutoPrintAfterSale()), updatedBy);
            if (p.getChangeGivingEnabled() != null) settingsService.setSetting(SettingKeys.POS_CHANGE_GIVING_ENABLED, bool(p.getChangeGivingEnabled()), updatedBy);
            if (p.getPaymentMethods() != null) {
                validatePaymentMethods(p.getPaymentMethods());
                settingsService.setSetting(SettingKeys.POS_PAYMENT_METHODS_ENABLED, serializePaymentMethods(p.getPaymentMethods()), updatedBy);
            }
            if (p.getAllowPartialPayment() != null) settingsService.setSetting(SettingKeys.POS_ALLOW_PARTIAL_PAYMENT, bool(p.getAllowPartialPayment()), updatedBy);
            if (p.getAllowSplitPayment() != null) settingsService.setSetting(SettingKeys.POS_ALLOW_SPLIT_PAYMENT, bool(p.getAllowSplitPayment()), updatedBy);
        }
        if (request.getStock() != null) {
            var s = request.getStock();
            if (s.getAllowNegativeStock() != null) settingsService.setSetting(SettingKeys.STOCK_ALLOW_NEGATIVE, bool(s.getAllowNegativeStock()), updatedBy);
            if (s.getLowStockThresholdDefault() != null) settingsService.setSetting(SettingKeys.STOCK_LOW_THRESHOLD, s.getLowStockThresholdDefault().toPlainString(), updatedBy);
            if (s.getValuationMethod() != null) settingsService.setSetting(SettingKeys.STOCK_VALUATION_METHOD, s.getValuationMethod().name(), updatedBy);
            if (s.getLowStockAlertsEnabled() != null) settingsService.setSetting(SettingKeys.STOCK_LOW_ALERTS_ENABLED, bool(s.getLowStockAlertsEnabled()), updatedBy);
            if (s.getMultiWarehouseEnabled() != null) settingsService.setSetting(SettingKeys.STOCK_MULTI_WAREHOUSE_ENABLED, bool(s.getMultiWarehouseEnabled()), updatedBy);
        }
        if (request.getTax() != null) {
            var t = request.getTax();
            if (t.getEnabled() != null) settingsService.setSetting(SettingKeys.TAX_ENABLED, bool(t.getEnabled()), updatedBy);
            if (t.getName() != null) settingsService.setSetting(SettingKeys.TAX_NAME, t.getName(), updatedBy);
            if (t.getDefaultRate() != null) settingsService.setSetting(SettingKeys.POS_TAX_RATE_DEFAULT, t.getDefaultRate().toPlainString(), updatedBy);
            if (t.getPricesIncludeTax() != null) settingsService.setSetting(SettingKeys.TAX_PRICES_INCLUDE_TAX, bool(t.getPricesIncludeTax()), updatedBy);
            if (t.getAutoApplyOnSales() != null) settingsService.setSetting(SettingKeys.TAX_AUTO_APPLY_ON_SALES, bool(t.getAutoApplyOnSales()), updatedBy);
        }
        return getClientConfiguration();
    }

    @Transactional
    public ClientConfigurationResponse uploadCompanyLogo(MultipartFile file, String updatedBy) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Fichier logo vide");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Le logo doit etre une image");
        }
        String previous = settingsService.getSetting(SettingKeys.COMPANY_LOGO);
        String stored = fileStorageService.store(file, "company/logo");
        settingsService.setSetting(SettingKeys.COMPANY_LOGO, stored, updatedBy);
        if (previous != null && !previous.isBlank() && !previous.startsWith("http")) {
            fileStorageService.delete(previous);
        }
        return getClientConfiguration();
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodSettingDto> parsePaymentMethods() {
        String json = settingsService.getSetting(SettingKeys.POS_PAYMENT_METHODS_ENABLED);
        try {
            List<PaymentMethodSettingDto> methods = objectMapper.readValue(json, new TypeReference<>() {});
            return methods != null ? methods : defaultPaymentMethods();
        } catch (Exception e) {
            return defaultPaymentMethods();
        }
    }

    @Transactional(readOnly = true)
    public boolean isPaymentMethodEnabled(String code) {
        return parsePaymentMethods().stream()
                .anyMatch(m -> m.getCode().equalsIgnoreCase(code) && m.isEnabled());
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveSaleLineTaxRate(BigDecimal requestedRate) {
        if (requestedRate != null) {
            return requestedRate;
        }
        if (!settingsService.getBoolean(SettingKeys.TAX_ENABLED)
                || !settingsService.getBoolean(SettingKeys.TAX_AUTO_APPLY_ON_SALES)) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = settingsService.getDecimal(SettingKeys.POS_TAX_RATE_DEFAULT);
        return rate != null ? rate : BigDecimal.ZERO;
    }

    private ClientConfigurationResponse.StockSection buildStockSection() {
        StockConfigResponse stock = settingsService.getStockConfig();
        return ClientConfigurationResponse.StockSection.builder()
                .allowNegativeStock(stock.isAllowNegativeStock())
                .lowStockThresholdDefault(stock.getLowStockThresholdDefault())
                .valuationMethod(stock.getValuationMethod())
                .lowStockAlertsEnabled(stock.isLowStockAlertsEnabled())
                .multiWarehouseEnabled(stock.isMultiWarehouseEnabled())
                .build();
    }

    private ClientConfigurationResponse.TaxSection buildTaxSection() {
        BigDecimal rate = settingsService.getDecimal(SettingKeys.POS_TAX_RATE_DEFAULT);
        return ClientConfigurationResponse.TaxSection.builder()
                .enabled(settingsService.getBoolean(SettingKeys.TAX_ENABLED))
                .name(settingsService.getSetting(SettingKeys.TAX_NAME))
                .defaultRate(rate != null ? rate : BigDecimal.ZERO)
                .pricesIncludeTax(settingsService.getBoolean(SettingKeys.TAX_PRICES_INCLUDE_TAX))
                .autoApplyOnSales(settingsService.getBoolean(SettingKeys.TAX_AUTO_APPLY_ON_SALES))
                .build();
    }

    private void setOptional(String key, String value, String updatedBy) {
        settingsService.setSetting(key, value != null ? value : "", updatedBy);
    }

    private static String bool(boolean value) {
        return Boolean.toString(value);
    }

    private void validatePaymentMethods(List<PaymentMethodSettingDto> methods) {
        if (methods.isEmpty()) {
            throw new BusinessException("Au moins un moyen de paiement doit etre configure");
        }
        boolean anyEnabled = methods.stream().anyMatch(PaymentMethodSettingDto::isEnabled);
        if (!anyEnabled) {
            throw new BusinessException("Au moins un moyen de paiement doit etre active");
        }
    }

    private String serializePaymentMethods(List<PaymentMethodSettingDto> methods) {
        try {
            return objectMapper.writeValueAsString(methods);
        } catch (Exception e) {
            throw new BusinessException("Configuration moyens de paiement invalide");
        }
    }

    static List<PaymentMethodSettingDto> defaultPaymentMethods() {
        return List.of(
                PaymentMethodSettingDto.builder().code("CASH").label("Especes").enabled(true).build(),
                PaymentMethodSettingDto.builder().code("CARD").label("Carte").enabled(true).build(),
                PaymentMethodSettingDto.builder().code("MOBILE_MONEY").label("Mobile money").enabled(true).build(),
                PaymentMethodSettingDto.builder().code("BANK_TRANSFER").label("Virement").enabled(false).build()
        );
    }
}
