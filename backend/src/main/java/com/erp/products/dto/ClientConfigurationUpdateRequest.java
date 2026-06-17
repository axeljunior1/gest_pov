package com.erp.products.dto;

import com.erp.products.domain.enums.StockValuationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ClientConfigurationUpdateRequest {

    @Valid
    private CompanySection company;
    @Valid
    private PosSection pos;
    @Valid
    private StockSection stock;
    @Valid
    private TaxSection tax;

    @Data
    public static class CompanySection {
        @NotBlank
        @Size(max = 200)
        private String name;
        @Size(max = 500)
        private String address;
        @Size(max = 120)
        private String city;
        @Size(max = 120)
        private String country;
        @Size(max = 40)
        private String phone;
        @Email
        @Size(max = 200)
        private String email;
        @Size(max = 80)
        private String taxId;
        private String currency;
        private String language;
        private String timezone;
        private String dateFormat;
    }

    @Data
    public static class PosSection {
        @Size(max = 120)
        private String registerName;
        @Size(max = 20)
        private String salePrefix;
        @Size(max = 32)
        private String ticketFormat;
        @Size(max = 1000)
        private String ticketFooter;
        private Boolean ticketShowLogo;
        private Boolean autoPrintAfterSale;
        private Boolean changeGivingEnabled;
        private List<PaymentMethodSettingDto> paymentMethods;
        private Boolean allowPartialPayment;
        private Boolean allowSplitPayment;
    }

    @Data
    public static class StockSection {
        private Boolean allowNegativeStock;
        private BigDecimal lowStockThresholdDefault;
        private StockValuationMethod valuationMethod;
        private Boolean lowStockAlertsEnabled;
        private Boolean multiWarehouseEnabled;
    }

    @Data
    public static class TaxSection {
        private Boolean enabled;
        @Size(max = 60)
        private String name;
        private BigDecimal defaultRate;
        private Boolean pricesIncludeTax;
        private Boolean autoApplyOnSales;
    }
}
