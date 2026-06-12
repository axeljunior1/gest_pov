package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicSettingsResponse {

    private String companyName;
    private String companyLogo;
    private String currency;
    private String language;
    private String timezone;
    private String dateFormat;
}
