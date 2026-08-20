package com.gestpov.desktop.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Critères GET /api/products — le filtrage réel est côté Spring Boot. */
public final class ProductQuery {

    public String query;
    public Long categorieId;
    public Long fournisseurId;
    public String marque;
    public String cycleVie;
    public Boolean stockFaible;
    public Boolean rupture;

    public Map<String, String> toParams() {
        Map<String, String> params = new LinkedHashMap<>();
        put(params, "query", query);
        if (categorieId != null) {
            params.put("categorieId", String.valueOf(categorieId));
        }
        if (fournisseurId != null) {
            params.put("fournisseurId", String.valueOf(fournisseurId));
        }
        put(params, "marque", marque);
        put(params, "cycleVie", cycleVie);
        if (Boolean.TRUE.equals(stockFaible)) {
            params.put("stockFaible", "true");
        }
        if (Boolean.TRUE.equals(rupture)) {
            params.put("rupture", "true");
        }
        return params;
    }

    private static void put(Map<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value.trim());
        }
    }
}
