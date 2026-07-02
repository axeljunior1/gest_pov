package com.erp.products.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public class BootstrapAdminProperties {

    private String email = "";
    private String password = "";
    private String firstName = "Admin";
    private String lastName = "ERP";
}
