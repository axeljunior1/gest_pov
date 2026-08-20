package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.ProductDraft;
import com.gestpov.desktop.model.ProductQuery;
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

class ProductClientTest {

    @Test
    void searchSeedAndFilter() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ProductClient client = client(server);
            List<Product> all = client.search(new ProductQuery());
            assertEquals(1, all.size());
            assertEquals("Cahier A4", all.get(0).nom());
            assertEquals(10, all.get(0).stockTotal());

            ProductQuery query = new ProductQuery();
            query.query = "Cahier";
            assertEquals(1, client.search(query).size());

            query.query = "Inconnu";
            assertTrue(client.search(query).isEmpty());

            ProductQuery byCat = new ProductQuery();
            byCat.categorieId = 1L;
            assertEquals(1, client.search(byCat).size());
        }
    }

    @Test
    void createUpdateDelete() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ProductClient client = client(server);
            ProductDraft draft = new ProductDraft();
            draft.nom = "Stylo";
            draft.prixVente = new BigDecimal("1.20");
            draft.generateBarcode = true;
            Product created = client.create(draft);
            assertEquals("Stylo", created.nom());
            assertEquals("1234567890123", created.codeBarre());
            assertEquals(2, client.search(new ProductQuery()).size());

            draft.nom = "Stylo bleu";
            Product updated = client.update(created.id(), draft);
            assertEquals("Stylo bleu", updated.nom());

            client.delete(created.id());
            assertEquals(1, client.search(new ProductQuery()).size());
        }
    }

    @Test
    void bulkDelete() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ProductClient client = client(server);
            ProductDraft draft = new ProductDraft();
            draft.nom = "Temp";
            Product extra = client.create(draft);
            int deleted = client.bulkDelete(List.of(extra.id()));
            assertEquals(1, deleted);
            assertEquals(1, client.search(new ProductQuery()).size());
        }
    }

    @Test
    void priceHistoryAndImage() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ProductClient client = client(server);
            Product seed = client.search(new ProductQuery()).get(0);
            Product priced = client.updatePrice(seed.id(), "VENTE", new BigDecimal("4.00"));
            assertEquals(0, priced.prixVente().compareTo(new BigDecimal("4.00")));
            assertEquals(1, client.priceHistory(seed.id()).size());

            var image = client.uploadImage(seed.id(), "a.png", new byte[] {1, 2, 3}, true);
            assertNotNull(image.id());
            Product withImage = client.getById(seed.id());
            assertEquals(1, withImage.images().size());
            client.deleteImage(seed.id(), image.id());
            assertTrue(client.getById(seed.id()).images().isEmpty());
        }
    }

    @Test
    void blankNameRejected() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ProductClient client = client(server);
            ProductDraft draft = new ProductDraft();
            draft.nom = "";
            ApiException ex = assertThrows(ApiException.class, () -> client.create(draft));
            assertEquals(400, ex.statusCode());
        }
    }

    @Test
    void withoutToken_401() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ProductClient client = new ProductClient(
                    new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2)));
            ApiException ex = assertThrows(ApiException.class, () -> client.search(new ProductQuery()));
            assertEquals(401, ex.statusCode());
        }
    }

    @Test
    void variantsPackagingsLifecycleAudit() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ProductClient client = client(server);
            Product seed = client.search(new ProductQuery()).get(0);

            var variant = client.addVariant(seed.id(), "Rouge", "M", "SKU-R-M", new BigDecimal("4.50"), true);
            assertNotNull(variant.id());
            assertEquals(1, client.listVariants(seed.id()).size());

            var pkg = client.addPackaging(seed.id(), "Carton", "ctn", new BigDecimal("12"), new BigDecimal("40"));
            assertNotNull(pkg.id());
            assertEquals(1, client.listPackagings(seed.id()).size());

            Product submitted = client.submitLifecycle(seed.id());
            assertEquals("EN_VALIDATION", submitted.cycleVie());
            Product approved = client.approveLifecycle(seed.id());
            assertEquals("VALIDE", approved.cycleVie());
            assertFalse(client.auditHistory(seed.id()).isEmpty());

            client.deleteVariant(seed.id(), variant.id());
            client.deletePackaging(seed.id(), pkg.id());
            assertTrue(client.listVariants(seed.id()).isEmpty());
            assertTrue(client.listPackagings(seed.id()).isEmpty());
        }
    }

    private static ProductClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new ProductClient(api);
    }
}
