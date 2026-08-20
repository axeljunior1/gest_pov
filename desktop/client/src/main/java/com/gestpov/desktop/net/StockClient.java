package com.gestpov.desktop.net;

import com.gestpov.desktop.model.StockItem;

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
}
