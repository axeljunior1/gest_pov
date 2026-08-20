package com.erp.products.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.desktop")
public class DesktopProperties {

    /**
     * Fichier UUID serveur. Vide = {app.license.data-dir}/server.id
     */
    private String serverIdFile = "";

    /** Nom affiché (vide = hostname machine). */
    private String serverName = "";

    /** Version API annoncée aux clients Desktop. */
    private String version = "1.0.0";

    private Discovery discovery = new Discovery();

    @Getter
    @Setter
    public static class Discovery {
        private boolean enabled = false;
        private int udpPort = 38471;
        private String probe = "GEST_POV_DISCOVERY";
    }
}
