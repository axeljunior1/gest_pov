package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Unit;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitClientTest {

    @Test
    void listCreateDelete() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            UnitClient client = client(server);
            List<Unit> all = client.findAll();
            assertEquals(1, all.size());
            assertEquals("pce", all.get(0).symbole());

            Unit created = client.create("Kilogramme", "kg");
            assertEquals("Kilogramme", created.nom());
            assertEquals(2, client.findAll().size());

            client.delete(created.id());
            assertEquals(1, client.findAll().size());
        }
    }

    @Test
    void withoutToken_401() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            UnitClient client = new UnitClient(new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2)));
            ApiException ex = assertThrows(ApiException.class, client::findAll);
            assertEquals(401, ex.statusCode());
        }
    }

    @Test
    void conversions() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            UnitClient client = client(server);
            Unit kg = client.create("Kilogramme", "kg");
            Unit g = client.create("Gramme", "g");
            var conv = client.createConversion(kg.id(), g.id(), new java.math.BigDecimal("1000"));
            assertNotNull(conv.id());
            assertEquals(1, client.listConversions().size());
            assertEquals(0, new java.math.BigDecimal("2").compareTo(client.convert(kg.id(), g.id(), new java.math.BigDecimal("2"))));
            client.deleteConversion(conv.id());
            assertEquals(0, client.listConversions().size());
        }
    }

    private static UnitClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new UnitClient(api);
    }
}
