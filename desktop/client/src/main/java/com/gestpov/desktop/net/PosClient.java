package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.Customer;
import com.gestpov.desktop.model.PosProduct;
import com.gestpov.desktop.model.Sale;

import java.math.BigDecimal;
import java.time.Instant;
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
        openSession(openingCash, "CASHIER");
    }

    public void openSession(BigDecimal openingCash, String sessionType) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        String type = sessionType == null || sessionType.isBlank() ? "CASHIER" : sessionType.trim();
        body.put("sessionType", type);
        body.put("openingCashAmount",
                "CASHIER".equals(type) ? (openingCash == null ? BigDecimal.ZERO : openingCash) : BigDecimal.ZERO);
        api.post("/api/pos/sessions/open", body);
    }

    public JsonNode closePreview() throws ApiException {
        return api.get("/api/pos/sessions/current/close-preview");
    }

    public JsonNode closeSession(BigDecimal closingCash) throws ApiException {
        return closeSession(closingCash, true, null, null, null, null);
    }

    public JsonNode closeSession(BigDecimal closingCash,
                                 boolean cancelPendingDrafts,
                                 String differenceReason,
                                 String differenceComment,
                                 String managerEmail,
                                 String managerPassword) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("closingCashAmount", closingCash == null ? BigDecimal.ZERO : closingCash);
        body.put("cancelPendingDrafts", cancelPendingDrafts);
        if (differenceReason != null && !differenceReason.isBlank()) {
            body.put("differenceReason", differenceReason.trim());
        }
        if (differenceComment != null && !differenceComment.isBlank()) {
            body.put("differenceComment", differenceComment.trim());
        }
        if (managerEmail != null && !managerEmail.isBlank()) {
            body.put("managerEmail", managerEmail.trim());
        }
        if (managerPassword != null && !managerPassword.isBlank()) {
            body.put("managerPassword", managerPassword);
        }
        return api.post("/api/pos/sessions/close", body);
    }

    public List<JsonNode> listClosedSessions(int limit) throws ApiException {
        return JsonLists.mapArray(api.get("/api/pos/sessions/closed", Map.of("limit", String.valueOf(limit))), n -> n);
    }

    public JsonNode sessionReport(long sessionId) throws ApiException {
        return api.get("/api/pos/sessions/" + sessionId + "/report");
    }

    public List<PosProduct> search(String q) throws ApiException {
        return search(q, 20);
    }

    public List<PosProduct> search(String q, int limit) throws ApiException {
        return PosProduct.fromSearch(api.get("/api/pos/catalog/search", Map.of(
                "q", q == null ? "" : q,
                "limit", String.valueOf(Math.max(1, Math.min(limit, 50)))
        )));
    }

    /** Aperçu catalogue (20 premiers) — fallback sur /catalog si search vide. */
    public List<PosProduct> browseCatalog(int limit) throws ApiException {
        int max = Math.max(1, Math.min(limit, 50));
        List<PosProduct> fromSearch = search("", max);
        if (!fromSearch.isEmpty()) {
            return fromSearch;
        }
        JsonNode node = api.get("/api/pos/catalog");
        List<PosProduct> all = PosProduct.fromSearch(node);
        if (all.size() <= max) {
            return all;
        }
        return all.subList(0, max);
    }

    public Sale scanItem(long saleId, String code, BigDecimal quantity) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code == null ? "" : code.trim());
        body.put("quantityInput", quantity == null ? BigDecimal.ONE : quantity);
        JsonNode node = api.post("/api/pos/sales/" + saleId + "/scan", body);
        return Sale.fromJson(node == null ? null : node.get("sale"));
    }

    public List<Customer> searchCustomers(String q) throws ApiException {
        return searchCustomers(q, 20);
    }

    public List<Customer> searchCustomers(String q, int limit) throws ApiException {
        return JsonLists.mapArray(api.get("/api/pos/customers/search", Map.of(
                "q", q == null ? "" : q,
                "limit", String.valueOf(Math.max(1, Math.min(limit, 50)))
        )), Customer::fromJson);
    }

    public Sale createSale() throws ApiException {
        return Sale.fromJson(api.post("/api/pos/sales", Map.of()));
    }

    public Sale getSale(long id) throws ApiException {
        return Sale.fromJson(api.get("/api/pos/sales/" + id));
    }

    public List<Sale> listCompletedSales(boolean sessionOnly, int limit) throws ApiException {
        return listCompletedSales(sessionOnly, limit, null, null);
    }

    public List<Sale> listCompletedSales(boolean sessionOnly, int limit, Instant dateFrom, Instant dateTo)
            throws ApiException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("sessionOnly", String.valueOf(sessionOnly));
        params.put("limit", String.valueOf(limit));
        if (dateFrom != null) {
            params.put("dateFrom", dateFrom.toString());
        }
        if (dateTo != null) {
            params.put("dateTo", dateTo.toString());
        }
        return JsonLists.mapArray(api.get("/api/pos/sales/completed", params), Sale::fromJson);
    }

    public List<Sale> listPendingPayments() throws ApiException {
        return JsonLists.mapArray(api.get("/api/pos/sales/pending-payment"), Sale::fromJson);
    }

    public Sale sendToPayment(long saleId) throws ApiException {
        return Sale.fromJson(api.post("/api/pos/sales/" + saleId + "/send-to-payment", Map.of()));
    }

    public Sale recallFromPayment(long saleId) throws ApiException {
        return Sale.fromJson(api.post("/api/pos/sales/" + saleId + "/recall-from-payment", Map.of()));
    }

    public Sale holdSale(long saleId, String label) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        if (label != null && !label.isBlank()) {
            body.put("label", label.trim());
        }
        return Sale.fromJson(api.post("/api/pos/sales/" + saleId + "/hold", body));
    }

    public Sale resumeSale(long saleId) throws ApiException {
        return Sale.fromJson(api.post("/api/pos/sales/" + saleId + "/resume", Map.of()));
    }

    public List<Sale> listHold() throws ApiException {
        return JsonLists.mapArray(api.get("/api/pos/sales/hold"), Sale::fromJson);
    }

    public Sale addLine(long saleId, long productId, Long variantId, BigDecimal quantity) throws ApiException {
        return addLine(saleId, productId, variantId, null, quantity);
    }

    public Sale addLine(long saleId, long productId, Long variantId, Long packagingId, BigDecimal quantity)
            throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("productId", productId);
        if (variantId != null) {
            body.put("variantId", variantId);
        }
        if (packagingId != null) {
            body.put("packagingId", packagingId);
        }
        body.put("quantityInput", quantity);
        return Sale.fromJson(api.post("/api/pos/sales/" + saleId + "/lines", body));
    }

    public Sale updateQty(long saleId, long lineId, BigDecimal quantity) throws ApiException {
        return Sale.fromJson(api.put("/api/pos/sales/" + saleId + "/lines/" + lineId, Map.of("quantity", quantity)));
    }

    public Sale removeLine(long saleId, long lineId) throws ApiException {
        return updateQty(saleId, lineId, BigDecimal.ZERO);
    }

    public Sale lineDiscount(long saleId, long lineId, BigDecimal amount) throws ApiException {
        return Sale.fromJson(api.put("/api/pos/sales/" + saleId + "/lines/" + lineId + "/discount",
                Map.of("discountAmount", amount)));
    }

    public Sale assignCustomer(long saleId, long customerId) throws ApiException {
        return Sale.fromJson(api.put("/api/pos/sales/" + saleId + "/customer", Map.of("customerId", customerId)));
    }

    /** Création rapide côté POS — seul le nom est obligatoire (prénom "Client" par défaut). */
    public Customer quickCreateCustomer(String lastName, String firstName, String phone) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("lastName", lastName);
        if (firstName != null && !firstName.isBlank()) {
            body.put("firstName", firstName);
        }
        if (phone != null && !phone.isBlank()) {
            body.put("phone", phone);
        }
        return Customer.fromJson(api.post("/api/pos/customers/quick", body));
    }

    public Sale clearCustomer(long saleId) throws ApiException {
        return Sale.fromJson(api.delete("/api/pos/sales/" + saleId + "/customer"));
    }

    public Sale redeemLoyalty(long saleId, int points) throws ApiException {
        return Sale.fromJson(api.post("/api/pos/sales/" + saleId + "/loyalty/redeem", Map.of("points", points)));
    }

    public Sale clearLoyalty(long saleId) throws ApiException {
        return Sale.fromJson(api.delete("/api/pos/sales/" + saleId + "/loyalty/redeem"));
    }

    public Sale validate(long saleId, String method, BigDecimal amount, BigDecimal cashReceived) throws ApiException {
        return validate(saleId, List.of(Map.of("method", method, "amount", amount)), cashReceived);
    }

    public Sale validate(long saleId, List<Map<String, Object>> payments, BigDecimal cashReceived) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payments", payments);
        if (cashReceived != null) {
            body.put("cashReceived", cashReceived);
        }
        return Sale.fromJson(api.post("/api/pos/sales/" + saleId + "/validate", body));
    }

    public JsonNode ticket(long saleId) throws ApiException {
        return api.get("/api/pos/sales/" + saleId + "/ticket");
    }

    public List<JsonNode> searchRefundable(String q, int limit) throws ApiException {
        return JsonLists.mapArray(api.get("/api/pos/sales/refundable/search", Map.of(
                "q", q == null ? "" : q,
                "limit", String.valueOf(limit)
        )), n -> n);
    }

    public JsonNode returnableSale(long saleId) throws ApiException {
        return api.get("/api/pos/sales/" + saleId + "/returnable");
    }

    public JsonNode createReturn(long saleId, String reason, boolean returnToStock) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        if (reason != null && !reason.isBlank()) {
            body.put("reason", reason.trim());
        }
        body.put("returnToStock", returnToStock);
        return api.post("/api/pos/sales/" + saleId + "/returns", body);
    }

    /** Retour partiel : une entree par ligne {saleLineId, quantity, restock}. */
    public JsonNode createReturn(long saleId, String reason, List<Map<String, Object>> lines) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        if (reason != null && !reason.isBlank()) {
            body.put("reason", reason.trim());
        }
        body.put("lines", lines == null ? List.of() : lines);
        return api.post("/api/pos/sales/" + saleId + "/returns", body);
    }

    public JsonNode validateReturn(long returnId, String method, BigDecimal amount) throws ApiException {
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("method", method);
        payment.put("amount", amount);
        return api.post("/api/pos/returns/" + returnId + "/validate", Map.of("payments", List.of(payment)));
    }

    public JsonNode returnReceipt(long returnId) throws ApiException {
        return api.get("/api/pos/returns/" + returnId + "/receipt");
    }
}
