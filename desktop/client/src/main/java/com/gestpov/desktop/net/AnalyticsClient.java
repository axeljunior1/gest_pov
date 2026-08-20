package com.gestpov.desktop.net;

import com.gestpov.desktop.model.AnalyticsOverview;
import com.gestpov.desktop.model.CancelledSale;

import java.util.List;
import java.util.Map;

public class AnalyticsClient {

    private final ApiClient api;

    public AnalyticsClient(ApiClient api) {
        this.api = api;
    }

    public AnalyticsOverview overview() throws ApiException {
        return AnalyticsOverview.fromJson(api.get("/api/analytics/overview"));
    }

    public List<CancelledSale> cancelledSales() throws ApiException {
        return JsonLists.mapArray(api.get("/api/sales/cancellations"), CancelledSale::fromJson);
    }

    public CancelledSale cancelledDetail(long id) throws ApiException {
        return CancelledSale.fromJson(api.get("/api/sales/cancellations/" + id));
    }
}
