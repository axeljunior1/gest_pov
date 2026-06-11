package com.erp.products.dto;

import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ProductSearchCriteria {

    private String query;
    private String sku;
    private String codeBarre;
    private Long categorieId;
    private String marque;
    private Long fournisseurId;
    private ProductStatus statut;
    private LifecycleStatus cycleVie;
    private Boolean stockFaible;
    private Boolean rupture;
    private BigDecimal prixMin;
    private BigDecimal prixMax;
    private Instant createdFrom;
    private Instant createdTo;
    private Instant updatedFrom;
    private Instant updatedTo;
    private Integer stockSeuil = 10;
}
