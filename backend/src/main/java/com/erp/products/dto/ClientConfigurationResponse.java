package com.erp.products.dto;

import com.erp.products.domain.enums.StockValuationMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ClientConfigurationResponse {

    private CompanySection company;
    private PosSection pos;
    private StockSection stock;
    private TaxSection tax;

    @Data
    @Builder
    public static class CompanySection {
        private String name;
        private String address;
        private String city;
        private String country;
        private String phone;
        private String email;
        private String taxId;
        private String logoPath;
        private String logoUrl;
        private String currency;
        private String language;
        private String timezone;
        private String dateFormat;
    }

    @Data
    @Builder
    public static class PosSection {
        private String registerName;
        private String salePrefix;
        private String ticketFormat;
        private String ticketFooter;
        private boolean ticketShowLogo;
        private boolean autoPrintAfterSale;
        private boolean changeGivingEnabled;
        private List<PaymentMethodSettingDto> paymentMethods;
        private boolean allowPartialPayment;
        private boolean allowSplitPayment;
    }

    @Data
    @Builder
    public static class StockSection {
        private boolean allowNegativeStock;
        private BigDecimal lowStockThresholdDefault;
        private StockValuationMethod valuationMethod;
        private boolean lowStockAlertsEnabled;
        private boolean multiWarehouseEnabled;
    }

    @Data
    @Builder
    public static class TaxSection {
        private boolean enabled;
        private String name;
        private BigDecimal defaultRate;
        private boolean pricesIncludeTax;
        private boolean autoApplyOnSales;
    }
}
