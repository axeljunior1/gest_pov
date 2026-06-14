package com.erp.products.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI erpOpenApi() {
        final String bearer = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("ERP Produits API")
                        .description("API REST — catalogue, stock, POS, ventes, analytics")
                        .version("1.0.0")
                        .contact(new Contact().name("ERP Produits")))
                .addSecurityItem(new SecurityRequirement().addList(bearer))
                .components(new Components().addSecuritySchemes(bearer,
                        new SecurityScheme()
                                .name(bearer)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT obtenu via POST /api/auth/login")));
    }

    @Bean
    public GroupedOpenApi posOpenApi() {
        return GroupedOpenApi.builder()
                .group("pos")
                .displayName("Point de vente (POS)")
                .pathsToMatch("/api/pos/**")
                .build();
    }

    @Bean
    public GroupedOpenApi salesOpenApi() {
        return GroupedOpenApi.builder()
                .group("sales")
                .displayName("Ventes & retours")
                .pathsToMatch("/api/sales/**")
                .build();
    }

    @Bean
    public GroupedOpenApi catalogOpenApi() {
        return GroupedOpenApi.builder()
                .group("catalog")
                .displayName("Catalogue & stock")
                .pathsToMatch("/api/products/**", "/api/categories/**", "/api/stock/**", "/api/warehouses/**")
                .build();
    }
}
