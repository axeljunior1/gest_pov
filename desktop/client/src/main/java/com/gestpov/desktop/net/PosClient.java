package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.PosProduct;
import com.gestpov.desktop.model.Sale;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PosClient {

    private final ApiClient api;

    public PosClient(ApiClient api) {
        this.api = api;
    }

    public JsonNode context() throws ApiException {
        return api.get("/api/pos/context");
    }

    public void openSession(BigDecimal openingCash) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("openingCashAmount", openingCash == null ? BigDecimal.ZERO : openingCash);
        api.post("/api/pos/sessions/open", body);
    }

    public List<PosProduct> search(String q) throws ApiException {
        return PosProduct.fromSearch(api.get("/api/pos/catalog/search", Map.of("q", q == null ? "" : q)));
    }

    public Sale createSale() throws ApiException {
        return Sale.fromJson(api.post("/api/pos/sales", Map.of()));
    }

    public Sale getSale(long id) throws ApiException {
        return Sale.fromJson(api.get("/api/pos/sales/" + id));
    }

    public Sale addLine(long saleId, long productId, Long variantId, BigDecimal quantity) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("productId", productId);
        if (variantId != null) {
            body.put("variantId", variantId);
        }
        body.put("quantityInput", quantity);
        return Sale.fromJson(api.post("/api/pos/sales/" + saleId + "/lines", body));
    }

    public Sale updateQty(long saleId, long lineId, BigDecimal quantity) throws ApiException {
        return Sale.fromJson(api.put("/api/pos/sales/" + saleId + "/lines/" + lineId, Map.of("quantity", quantity)));
    }

    public Sale lineDiscount(long saleId, long lineId, BigDecimal amount) throws ApiException {
        return Sale.fromJson(api.put("/api/pos/sales/" + saleId + "/lines/" + lineId + "/discount",
                Map.of("discountAmount", amount)));
    }

    public Sale validate(long saleId, String method, BigDecimal amount, BigDecimal cashReceived) throws ApiException {
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("method", method);
        payment.put("amount", amount);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payments", List.of(payment));
        if (cashReceived != null) {
            body.put("cashReceived", cashReceived);
        }
        return Sale.fromJson(api.post("/api/pos/sales/" + saleId + "/validate", body));
    }

    public JsonNode ticket(long saleId) throws ApiException {
        return api.get("/api/pos/sales/" + saleId + "/ticket");
    }
}
