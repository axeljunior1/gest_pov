package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Brand;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrandClientTest {

    @Test
    void mappingAndRefreshAfterCreate() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            BrandClient client = client(server);
            List<Brand> initial = client.findAll();
            assertEquals("Nike", initial.get(0).nom());

            Brand created = client.create("Adidas");
            assertEquals("Adidas", created.nom());
            assertEquals(2, client.findAll().size());
        }
    }

    @Test
    void search() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            BrandClient client = client(server);
            client.create("Puma");
            List<Brand> found = client.search("pu");
            assertEquals(1, found.size());
            assertEquals("Puma", found.get(0).nom());
        }
    }

    @Test
    void update() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            BrandClient client = client(server);
            Brand nike = client.findAll().get(0);
            Brand updated = client.update(nike.id(), "Nike Official");
            assertEquals("Nike Official", updated.nom());
        }
    }

    @Test
    void duplicateName_businessError() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            BrandClient client = client(server);
            ApiException ex = assertThrows(ApiException.class, () -> client.create("Nike"));
            assertEquals(400, ex.statusCode());
            assertTrue(ex.getMessage().contains("existe déjà"));
            assertEquals("Une marque avec ce nom existe déjà", ApiException.userMessage(ex));
        }
    }

    @Test
    void deleteOk_andLinkedRejected() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            BrandClient client = client(server);
            Brand extra = client.create("Temp");
            client.delete(extra.id());
            assertEquals(1, client.findAll().size());

            Brand linked = client.create("Linked");
            ApiException ex = assertThrows(ApiException.class, () -> client.delete(linked.id()));
            assertEquals(400, ex.statusCode());
            assertTrue(ex.getMessage().contains("liée à des produits"));
        }
    }

    @Test
    void brandsWithoutToken_401() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            BrandClient client = new BrandClient(api);
            ApiException ex = assertThrows(ApiException.class, client::findAll);
            assertEquals(401, ex.statusCode());
        }
    }

    private static BrandClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new BrandClient(api);
    }
}
