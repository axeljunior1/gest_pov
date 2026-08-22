package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.InventoryCount;
import com.gestpov.desktop.model.PurchaseOrder;
import com.gestpov.desktop.model.StockEntryDoc;
import com.gestpov.desktop.model.StockExitDoc;
import com.gestpov.desktop.model.StockItem;
import com.gestpov.desktop.model.StockLocation;
import com.gestpov.desktop.model.StockMovement;
import com.gestpov.desktop.model.StockTransfer;
import com.gestpov.desktop.model.StockValuationOverview;
import com.gestpov.desktop.model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StockClient {

    private final ApiClient api;

    public StockClient(ApiClient api) {
        this.api = api;
    }

    public List<StockItem> listItems() throws ApiException {
        return JsonLists.mapArray(api.get("/api/stock/items"), StockItem::fromJson);
    }

    public List<StockItem> listItems(Long warehouseId) throws ApiException {
        if (warehouseId == null) {
            return listItems();
        }
        return JsonLists.mapArray(api.get("/api/stock/items", Map.of("warehouseId", String.valueOf(warehouseId))),
                StockItem::fromJson);
    }

    public List<Warehouse> listWarehouses() throws ApiException {
        return JsonLists.mapArray(api.get("/api/warehouses"), Warehouse::fromJson);
    }

    public Warehouse createWarehouse(String code, String nom, String adresse) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code == null ? "" : code.trim());
        body.put("nom", nom == null ? "" : nom.trim());
        if (adresse != null && !adresse.isBlank()) {
            body.put("adresse", adresse.trim());
        }
        body.put("actif", true);
        return Warehouse.fromJson(api.post("/api/warehouses", body));
    }

    public List<StockLocation> listLocations(long warehouseId) throws ApiException {
        return JsonLists.mapArray(api.get("/api/warehouses/" + warehouseId + "/locations"), StockLocation::fromJson);
    }

    public StockLocation createLocation(long warehouseId, String code, String nom) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code == null ? "" : code.trim());
        body.put("nom", nom == null ? "" : nom.trim());
        body.put("actif", true);
        return StockLocation.fromJson(api.post("/api/warehouses/" + warehouseId + "/locations", body));
    }

    public List<StockMovement> listMovements() throws ApiException {
        return JsonLists.mapArray(api.get("/api/stock/movements", Map.of("limit", "100")), StockMovement::fromJson);
    }

    public List<StockMovement> listMovements(Long productId, Long warehouseId, LocalDate dateFrom, LocalDate dateTo)
            throws ApiException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("limit", "200");
        if (productId != null) {
            params.put("productId", String.valueOf(productId));
        }
        if (warehouseId != null) {
            params.put("warehouseId", String.valueOf(warehouseId));
        }
        if (dateFrom != null) {
            params.put("dateFrom", dateFrom.toString());
        }
        if (dateTo != null) {
            params.put("dateTo", dateTo.toString());
        }
        return JsonLists.mapArray(api.get("/api/stock/movements", params), StockMovement::fromJson);
    }

    public List<StockTransfer> listTransfers() throws ApiException {
        return JsonLists.mapArray(api.get("/api/stock/transfers"), StockTransfer::fromJson);
    }

    public StockTransfer createTransfer(String reference,
                                        long sourceWarehouseId,
                                        long destWarehouseId,
                                        long productId,
                                        BigDecimal quantity,
                                        long sourceLocationId,
                                        long destLocationId,
                                        String notes) throws ApiException {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("productId", productId);
        line.put("quantity", quantity);
        line.put("sourceLocationId", sourceLocationId);
        line.put("destLocationId", destLocationId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reference", reference == null ? "" : reference.trim());
        body.put("sourceWarehouseId", sourceWarehouseId);
        body.put("destWarehouseId", destWarehouseId);
        if (notes != null && !notes.isBlank()) {
            body.put("notes", notes.trim());
        }
        body.put("lignes", List.of(line));
        return StockTransfer.fromJson(api.post("/api/stock/transfers", body));
    }

    public StockTransfer shipTransfer(long id) throws ApiException {
        return StockTransfer.fromJson(api.post("/api/stock/transfers/" + id + "/ship", Map.of()));
    }

    public StockTransfer receiveTransfer(long id) throws ApiException {
        return StockTransfer.fromJson(api.post("/api/stock/transfers/" + id + "/receive", Map.of()));
    }

    public List<StockEntryDoc> listEntries() throws ApiException {
        return JsonLists.mapArray(api.get("/api/stock/entries"), StockEntryDoc::fromJson);
    }

    public List<StockExitDoc> listExits() throws ApiException {
        return JsonLists.mapArray(api.get("/api/stock/exits"), StockExitDoc::fromJson);
    }

    public List<InventoryCount> listInventories() throws ApiException {
        return JsonLists.mapArray(api.get("/api/stock/inventories"), InventoryCount::fromJson);
    }

    public StockValuationOverview getValuationOverview() throws ApiException {
        return StockValuationOverview.fromJson(api.get("/api/stock/valuation/overview"));
    }

    public BigDecimal getCurrentValuation() throws ApiException {
        JsonNode node = api.get("/api/stock/valuation/current");
        if (node == null || node.isNull()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public List<PurchaseOrder> listPurchaseOrders() throws ApiException {
        return JsonLists.mapArray(api.get("/api/purchase-orders"), PurchaseOrder::fromJson);
    }

    public List<PurchaseOrder> listPurchaseOrders(String status, Long supplierId) throws ApiException {
        Map<String, String> params = new LinkedHashMap<>();
        if (status != null && !status.isBlank()) {
            params.put("status", status);
        }
        if (supplierId != null) {
            params.put("supplierId", String.valueOf(supplierId));
        }
        return JsonLists.mapArray(api.get("/api/purchase-orders", params), PurchaseOrder::fromJson);
    }

    public PurchaseOrder createPurchaseOrder(long supplierId, Long warehouseId, LocalDate expectedDeliveryDate,
                                             String notes, long productId, BigDecimal quantity) throws ApiException {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("productId", productId);
        line.put("quantity", quantity);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("supplierId", supplierId);
        if (warehouseId != null) {
            body.put("warehouseId", warehouseId);
        }
        body.put("expectedDeliveryDate", expectedDeliveryDate == null ? LocalDate.now().plusDays(7) : expectedDeliveryDate);
        if (notes != null && !notes.isBlank()) {
            body.put("notes", notes.trim());
        }
        body.put("lines", List.of(line));
        return PurchaseOrder.fromJson(api.post("/api/purchase-orders", body));
    }

    public JsonNode createExit(long warehouseId, long locationId, String reason, String notes,
                               List<Map<String, Object>> lines) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("warehouseId", warehouseId);
        body.put("locationId", locationId);
        body.put("reason", reason);
        if (notes != null && !notes.isBlank()) {
            body.put("notes", notes.trim());
        }
        body.put("lignes", lines);
        return api.post("/api/stock/exits", body);
    }

    public JsonNode validateExit(long id) throws ApiException {
        return api.post("/api/stock/exits/" + id + "/validate", Map.of());
    }

    public JsonNode cancelExit(long id) throws ApiException {
        return api.post("/api/stock/exits/" + id + "/cancel", Map.of());
    }

    public void receipt(long productId, long warehouseId, long locationId, BigDecimal quantityBase, String reference)
            throws ApiException {
        api.post("/api/stock/receipt", operation(productId, warehouseId, locationId, quantityBase, reference));
    }

    public void issue(long productId, long warehouseId, long locationId, BigDecimal quantityBase, String reference)
            throws ApiException {
        api.post("/api/stock/issue", operation(productId, warehouseId, locationId, quantityBase, reference));
    }

    public void adjust(long productId, long warehouseId, long locationId, BigDecimal quantityBase, String reference)
            throws ApiException {
        api.post("/api/stock/adjust", operation(productId, warehouseId, locationId, quantityBase, reference));
    }

    private static Map<String, Object> operation(long productId, long warehouseId, long locationId,
                                                 BigDecimal quantityBase, String reference) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("productId", productId);
        body.put("warehouseId", warehouseId);
        body.put("locationId", locationId);
        body.put("quantityBase", quantityBase);
        if (reference != null && !reference.isBlank()) {
            body.put("reference", reference.trim());
        }
        return body;
    }
}
