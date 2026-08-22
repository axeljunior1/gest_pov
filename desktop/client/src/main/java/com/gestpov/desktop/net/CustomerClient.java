package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Customer;
import com.gestpov.desktop.model.CustomerHistory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomerClient {

    private final ApiClient api;

    public CustomerClient(ApiClient api) {
        this.api = api;
    }

    public List<Customer> list() throws ApiException {
        return JsonLists.mapArray(api.get("/api/customers"), Customer::fromJson);
    }

    public List<Customer> search(String q) throws ApiException {
        return JsonLists.mapArray(api.get("/api/customers/search", Map.of("q", q == null ? "" : q)),
                Customer::fromJson);
    }

    public List<Customer> listInactive(int days) throws ApiException {
        return JsonLists.mapArray(api.get("/api/customers/inactive", Map.of("days", String.valueOf(days))),
                Customer::fromJson);
    }

    public byte[] exportCsv(String format) throws ApiException {
        return api.getBytes("/api/export/customers", Map.of("format", format == null ? "CSV" : format));
    }

    public Customer getById(long id) throws ApiException {
        return Customer.fromJson(api.get("/api/customers/" + id));
    }

    public CustomerHistory history(long id) throws ApiException {
        return CustomerHistory.fromJson(api.get("/api/customers/" + id + "/history"));
    }

    public Customer adjustPoints(long id, int points, String reason) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("points", points);
        body.put("reason", reason == null ? "" : reason);
        return Customer.fromJson(api.post("/api/customers/" + id + "/loyalty/adjust", body));
    }

    public Customer create(Customer customer) throws ApiException {
        return Customer.fromJson(api.post("/api/customers", body(customer)));
    }

    public Customer update(long id, Customer customer) throws ApiException {
        return Customer.fromJson(api.put("/api/customers/" + id, body(customer)));
    }

    public void delete(long id) throws ApiException {
        api.delete("/api/customers/" + id);
    }

    private static Map<String, Object> body(Customer customer) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", customer.firstName());
        body.put("lastName", customer.lastName());
        body.put("phone", emptyToNull(customer.phone()));
        body.put("email", emptyToNull(customer.email()));
        body.put("companyName", emptyToNull(customer.companyName()));
        body.put("address", emptyToNull(customer.address()));
        body.put("city", emptyToNull(customer.city()));
        return body;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
