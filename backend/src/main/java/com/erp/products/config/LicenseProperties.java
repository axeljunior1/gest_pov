package com.erp.products.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.license")
public class LicenseProperties {

    /** Dossier local : installation.id + gest_pov.lic */
    private String dataDir = "./gest-pov-data";

    /** Nom du fichier licence dans dataDir */
    private String licenseFileName = "gest_pov.lic";

    /** Nom du fichier identifiant d'installation */
    private String installationIdFileName = "installation.id";

    /** Chemin classpath de la clé publique RSA (resources) */
    private String publicKeyResource = "classpath:keys/public_key.pem";

    /** Bloquer les endpoints métier si licence absente/invalide */
    private boolean enforcementEnabled = true;

    /** Nom d'application attendu dans le payload (optionnel côté émetteur) */
    private String expectedApp = "gest_pov";
}
