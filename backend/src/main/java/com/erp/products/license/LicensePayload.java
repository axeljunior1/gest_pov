package com.erp.products.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicensePayload {

    private String licenseId;
    private String client;
    private String site;
    private String installationId;
    private Integer maxUsers;
    private String issuedAt;
    private String expiresAt;
    /** Identifiant produit — doit valoir gest_pov si présent */
    private String app;
}
