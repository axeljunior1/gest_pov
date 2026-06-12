package com.erp.products.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret = "erp-dev-secret-change-in-production-min-256-bits-long-key!!";
    private long expirationMs = 86_400_000L;
}
