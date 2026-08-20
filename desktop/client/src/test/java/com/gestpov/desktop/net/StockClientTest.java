package com.gestpov.desktop.net;

import com.gestpov.desktop.model.InventoryCount;
import com.gestpov.desktop.model.PurchaseOrder;
import com.gestpov.desktop.model.StockEntryDoc;
import com.gestpov.desktop.model.StockExitDoc;
import com.gestpov.desktop.model.StockItem;
import com.gestpov.desktop.model.StockLocation;
import com.gestpov.desktop.model.StockTransfer;
import com.gestpov.desktop.model.StockValuationOverview;
import com.gestpov.desktop.model.Warehouse;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void warehousesLocationsAndReceipt() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            StockClient client = client(server);
            List<Warehouse> wh = client.listWarehouses();
            assertFalse(wh.isEmpty());
            List<StockLocation> locs = client.listLocations(wh.get(0).id());
            assertFalse(locs.isEmpty());

            StockItem before = client.listItems().get(0);
            client.receipt(before.productId(), before.warehouseId(), before.locationId(), new BigDecimal("5"), "BON-1");
            StockItem after = client.listItems().get(0);
            assertEquals(0, new BigDecimal("15.00").compareTo(after.quantityOnHand()));
        }
    }

    @Test
    void createWarehouseAndLocation() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            StockClient client = client(server);
            Warehouse created = client.createWarehouse("DEPOT", "Depot nord", "Rue 1");
            assertEquals("DEPOT", created.code());
            StockLocation loc = client.createLocation(1L, "B-01", "Zone B");
            assertEquals("B-01", loc.code());
        }
    }

    @Test
    void phaseBListsAndTransfers() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            StockClient client = client(server);

            List<StockTransfer> transfers = client.listTransfers();
            assertEquals(1, transfers.size());
            assertEquals("TR-1", transfers.get(0).reference());

            StockTransfer created = client.createTransfer("TR-X", 1L, 2L, 1L, new BigDecimal("2"), 1L, 1L, null);
            assertEquals("DRAFT", created.status());

            assertEquals("SHIPPED", client.shipTransfer(1L).status());
            assertEquals("RECEIVED", client.receiveTransfer(1L).status());

            List<StockEntryDoc> entries = client.listEntries();
            assertFalse(entries.isEmpty());
            List<StockExitDoc> exits = client.listExits();
            assertFalse(exits.isEmpty());
            List<InventoryCount> inventories = client.listInventories();
            assertFalse(inventories.isEmpty());

            StockValuationOverview overview = client.getValuationOverview();
            assertTrue(overview.totalStockValue().compareTo(BigDecimal.ZERO) > 0);
            assertFalse(overview.byCategory().isEmpty());
            assertEquals(0, new BigDecimal("1250.50").compareTo(client.getCurrentValuation()));

            List<PurchaseOrder> orders = client.listPurchaseOrders();
            assertEquals(1, orders.size());
            assertEquals("BC-1", orders.get(0).reference());
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
