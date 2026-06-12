package com.erp.products.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.stock")
@Getter
@Setter
public class StockProperties {

    /** Autoriser un stock physique négatif (déconseillé en production). */
    private boolean allowNegativeStock = false;
}
