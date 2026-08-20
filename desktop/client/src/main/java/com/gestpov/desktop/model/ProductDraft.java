package com.gestpov.desktop.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Corps POST/PUT /api/products. Pas de règle métier. */
public final class ProductDraft {

    public String nom;
    public String sku;
    public String codeBarre;
    public boolean generateBarcode;
    public String description;
    public Long marqueId;
    public Long categorieId;
    public Long fournisseurPrincipalId;
    public Long unitId;
    public BigDecimal prixAchat;
    public BigDecimal prixVente;
    public BigDecimal prixPromotionnel;
    public String statut = "ACTIF";
    public String cycleVie = "BROUILLON";

    public Map<String, Object> toBody(boolean isNew) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nom", nom);
        if (sku != null && !sku.isBlank()) {
            body.put("sku", sku);
        }
        if (description != null && !description.isBlank()) {
            body.put("description", description);
        }
        if (marqueId != null) {
            body.put("marqueId", marqueId);
        }
        if (categorieId != null) {
            body.put("categorieId", categorieId);
        }
        if (fournisseurPrincipalId != null) {
            body.put("fournisseurPrincipalId", fournisseurPrincipalId);
        }
        if (unitId != null) {
            body.put("unitId", unitId);
        }
        if (prixAchat != null) {
            body.put("prixAchat", prixAchat);
        }
        if (prixVente != null) {
            body.put("prixVente", prixVente);
        }
        if (prixPromotionnel != null) {
            body.put("prixPromotionnel", prixPromotionnel);
        }
        if (statut != null) {
            body.put("statut", statut);
        }
        if (cycleVie != null) {
            body.put("cycleVie", cycleVie);
        }
        if (codeBarre != null && !codeBarre.isBlank()) {
            body.put("codeBarre", codeBarre);
        } else if (isNew || generateBarcode) {
            body.put("generateBarcode", true);
        }
        return body;
    }

    public static ProductDraft from(Product product) {
        ProductDraft draft = new ProductDraft();
        if (product == null) {
            draft.generateBarcode = true;
            return draft;
        }
        draft.nom = product.nom();
        draft.sku = product.sku();
        draft.codeBarre = product.codeBarre();
        draft.generateBarcode = false;
        draft.description = product.description();
        draft.marqueId = product.marqueId();
        draft.categorieId = product.categorieId();
        draft.fournisseurPrincipalId = product.fournisseurPrincipalId();
        draft.unitId = product.unitId();
        draft.prixAchat = product.prixAchat();
        draft.prixVente = product.prixVente();
        draft.prixPromotionnel = product.prixPromotionnel();
        draft.statut = product.statut() == null ? "ACTIF" : product.statut();
        draft.cycleVie = product.cycleVie() == null ? "BROUILLON" : product.cycleVie();
        return draft;
    }
}
