package com.gestpov.desktop.net;

import com.gestpov.desktop.model.PosProduct;
import com.gestpov.desktop.model.Sale;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PosClientTest {

    @Test
    void sessionSearchCartValidateTicket() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            PosClient client = client(server);
            assertTrue(client.context().get("session").isNull());
            client.openSession(new BigDecimal("50"), "CASHIER");
            var ctx = client.context();
            assertFalse(ctx.get("session").isNull());
            assertEquals("CASHIER", ctx.get("session").get("sessionType").asText());

            List<PosProduct> found = client.search("Cahier");
            assertEquals(1, found.size());
            assertEquals("Cahier A4", found.get(0).nom());

            Sale sale = client.createSale();
            assertEquals("DRAFT", sale.status());
            sale = client.addLine(sale.id(), found.get(0).id(), null, new BigDecimal("2"));
            assertEquals(1, sale.lignes().size());
            assertEquals(0, new BigDecimal("7.00").compareTo(sale.total()));
            long lineId = sale.lignes().get(0).id();

            sale = client.updateQty(sale.id(), lineId, new BigDecimal("3"));
            assertEquals(0, new BigDecimal("10.50").compareTo(sale.total()));

            sale = client.lineDiscount(sale.id(), lineId, new BigDecimal("0.50"));
            assertEquals(0, new BigDecimal("10.00").compareTo(sale.total()));

            Sale paid = client.validate(sale.id(), "CASH", sale.total(), new BigDecimal("20"));
            assertEquals("COMPLETED", paid.status());
            assertNotNull(client.ticket(paid.id()).get("saleNumber"));

            var preview = client.closePreview();
            assertEquals(1, preview.path("saleCount").asInt());
        }
    }

    @Test
    void sendToPaymentAndPending() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            PosClient client = client(server);
            client.openSession(BigDecimal.ZERO, "SALES");
            Sale sale = client.createSale();
            var found = client.search("Cahier");
            sale = client.addLine(sale.id(), found.get(0).id(), null, BigDecimal.ONE);
            sale = client.sendToPayment(sale.id());
            assertEquals("PENDING_PAYMENT", sale.status());
            assertEquals(1, client.listPendingPayments().size());
            client.recallFromPayment(sale.id());
            assertEquals(0, client.listPendingPayments().size());
        }
    }

    @Test
    void removeLineAssignCustomerAndClose() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            PosClient client = client(server);
            client.openSession(BigDecimal.ZERO);
            Sale sale = client.createSale();
            var found = client.search("Cahier");
            sale = client.addLine(sale.id(), found.get(0).id(), null, new BigDecimal("1"));
            long lineId = sale.lignes().get(0).id();
            sale = client.removeLine(sale.id(), lineId);
            assertTrue(sale.lignes().isEmpty());

            sale = client.addLine(sale.id(), found.get(0).id(), null, BigDecimal.ONE);
            var customers = client.searchCustomers("Marie");
            assertFalse(customers.isEmpty());
            sale = client.assignCustomer(sale.id(), customers.get(0).id());
            assertEquals(customers.get(0).id(), sale.customerId());
            sale = client.clearCustomer(sale.id());
            assertTrue(sale.customerId() == null);

            client.closeSession(BigDecimal.ZERO);
            assertTrue(client.context().get("session").isNull());
        }
    }

    @Test
    void withoutToken_401() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            PosClient client = new PosClient(new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2)));
            ApiException ex = assertThrows(ApiException.class, client::context);
            assertEquals(401, ex.statusCode());
        }
    }

    private static PosClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new PosClient(api);
    }
}
