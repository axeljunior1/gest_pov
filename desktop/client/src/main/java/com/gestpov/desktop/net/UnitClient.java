package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.Unit;
import com.gestpov.desktop.model.UnitConversion;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UnitClient {

    private final ApiClient api;

    public UnitClient(ApiClient api) {
        this.api = api;
    }

    public List<Unit> findAll() throws ApiException {
        return JsonLists.mapArray(api.get("/api/units"), Unit::fromJson);
    }

    public Unit create(String nom, String symbole) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nom", nom);
        body.put("symbole", symbole);
        return Unit.fromJson(api.post("/api/units", body));
    }

    public void delete(long id) throws ApiException {
        api.delete("/api/units/" + id);
    }

    public List<UnitConversion> listConversions() throws ApiException {
        return JsonLists.mapArray(api.get("/api/units/conversions"), UnitConversion::fromJson);
    }

    public UnitConversion createConversion(long fromUnitId, long toUnitId, BigDecimal factor) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fromUnitId", fromUnitId);
        body.put("toUnitId", toUnitId);
        body.put("factor", factor);
        return UnitConversion.fromJson(api.post("/api/units/conversions", body));
    }

    public void deleteConversion(long id) throws ApiException {
        api.delete("/api/units/conversions/" + id);
    }

    public BigDecimal convert(long fromUnitId, long toUnitId, BigDecimal quantity) throws ApiException {
        JsonNode node = api.get("/api/units/convert", Map.of(
                "fromUnitId", String.valueOf(fromUnitId),
                "toUnitId", String.valueOf(toUnitId),
                "quantity", quantity.toPlainString()
        ));
        if (node == null || !node.hasNonNull("result")) {
            return null;
        }
        try {
            return new BigDecimal(node.get("result").asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
