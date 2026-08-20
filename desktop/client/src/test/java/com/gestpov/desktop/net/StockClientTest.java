package com.gestpov.desktop.net;

import com.gestpov.desktop.model.StockItem;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockClientTest {

    @Test
    void listItems() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            StockClient client = client(server);
            List<StockItem> items = client.listItems();
            assertEquals(1, items.size());
            assertEquals("Cahier A4", items.get(0).productNom());
            assertEquals(new BigDecimal("10"), items.get(0).quantityOnHand());
            assertEquals("PRINCIPAL", items.get(0).warehouseCode());
        }
    }

    @Test
    void withoutToken_401() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            StockClient client = new StockClient(new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2)));
            ApiException ex = assertThrows(ApiException.class, client::listItems);
            assertEquals(401, ex.statusCode());
        }
    }

    private static StockClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new StockClient(api);
    }
}
