package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicSettingsResponse {

    private String companyName;
    private String companyLogo;
    private String companyLogoUrl;
    private String companyAddress;
    private String companyCity;
    private String companyCountry;
    private String companyPhone;
    private String companyEmail;
    private String companyTaxId;
    private String currency;
    private String language;
    private String timezone;
    private String dateFormat;
    private String taxName;
    private boolean taxEnabled;
    private boolean pricesIncludeTax;
    private boolean setupCompleted;
}
