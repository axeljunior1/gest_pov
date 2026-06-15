package com.erp.products.license;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LicenseStatusResponse {

    private boolean valid;
    private boolean activated;
    private String reason;
    private String licenseId;
    private String client;
    private String site;
    private String issuedAt;
    private String expiresAt;
    private Long daysRemaining;
    private Integer maxUsers;
    private String installationId;
}
