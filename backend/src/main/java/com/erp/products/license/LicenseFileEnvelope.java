package com.erp.products.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseFileEnvelope {

    private String payload;
    private String signature;
}
