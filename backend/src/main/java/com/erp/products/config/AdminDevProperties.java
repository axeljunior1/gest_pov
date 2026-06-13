package com.erp.products.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.admin")
public class AdminDevProperties {

    /** Active uniquement en profil dev (voir application-dev.yml). */
    private boolean resetEnabled = false;

    private String resetToken = "dev-reset-token-change-me";
}
