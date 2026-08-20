package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductMappingTest {

    @Test
    void fromJsonListItem() throws Exception {
        var node = new ObjectMapper().readTree("""
                {"id":9,"nom":"Cahier","sku":"CAH","marque":"Nike","categorieNom":"Papeterie",
                 "prixVente":3.5,"stockTotal":4,"statut":"ACTIF","cycleVie":"BROUILLON",
                 "baseUnitSymbole":"pce","images":[{"id":1,"fileName":"a.png","url":"/uploads/a.png","principale":true}]}
                """);
        Product product = Product.fromJson(node);
        assertEquals(9L, product.id());
        assertEquals("Cahier", product.nom());
        assertEquals(new BigDecimal("3.5"), product.prixVente());
        assertEquals("4 pce", product.stockLabel());
        assertEquals(1, product.images().size());
        assertFalse(product.hasVariants());
    }
}
