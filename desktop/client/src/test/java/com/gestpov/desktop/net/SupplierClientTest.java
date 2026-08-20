package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Supplier;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierClientTest {

    @Test
    void listSearchCreateUpdateDelete() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            SupplierClient client = client(server);
            List<Supplier> all = client.findAll();
            assertEquals(1, all.size());
            assertEquals("Grossiste Nord", all.get(0).nom());

            Supplier created = client.create(new Supplier(null, "Local", "a@b.c", "01", "Rue 1"));
            assertEquals("Local", created.nom());
            assertEquals("a@b.c", created.email());
            assertEquals(2, client.findAll().size());

            List<Supplier> found = client.search("loc");
            assertEquals(1, found.size());

            Supplier updated = client.update(created.id(), new Supplier(created.id(), "Local Plus", "x@y.z", "02", "Rue 2"));
            assertEquals("Local Plus", updated.nom());

            client.delete(created.id());
            assertEquals(1, client.findAll().size());
        }
    }

    @Test
    void withoutToken_401() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            SupplierClient client = new SupplierClient(new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2)));
            ApiException ex = assertThrows(ApiException.class, client::findAll);
            assertEquals(401, ex.statusCode());
        }
    }

    @Test
    void emptyName_400() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            SupplierClient client = client(server);
            ApiException ex = assertThrows(ApiException.class,
                    () -> client.create(new Supplier(null, "", "", "", "")));
            assertEquals(400, ex.statusCode());
        }
    }

    private static SupplierClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new SupplierClient(api);
    }
}
