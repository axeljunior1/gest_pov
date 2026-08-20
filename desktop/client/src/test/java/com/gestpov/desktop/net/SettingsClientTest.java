package com.gestpov.desktop.net;

import com.gestpov.desktop.model.AppSetting;
import com.gestpov.desktop.model.ReferenceValueOption;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsClientTest {

    @Test
    void getAllAndUpdate() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            SettingsClient client = client(server);
            List<AppSetting> all = client.getAll();
            assertTrue(all.size() >= 2);
            AppSetting company = all.stream().filter(s -> "company.name".equals(s.key())).findFirst().orElseThrow();
            assertEquals("Gest POV Test", company.value());

            AppSetting updated = client.update("company.name", "Boutique Nord");
            assertEquals("Boutique Nord", updated.value());
            assertEquals("Boutique Nord",
                    client.getAll().stream().filter(s -> "company.name".equals(s.key())).findFirst().orElseThrow().value());
        }
    }

    @Test
    void referenceValuesAndBulkUpdate() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            SettingsClient client = client(server);
            Map<String, List<ReferenceValueOption>> refs = client.getReferenceValues();
            assertTrue(refs.containsKey("CURRENCY"));
            assertFalse(refs.get("CURRENCY").isEmpty());
            assertEquals("EUR", refs.get("CURRENCY").get(0).code());

            AppSetting currency = client.getAll().stream()
                    .filter(s -> "app.currency".equals(s.key()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("CURRENCY", currency.referenceCategory());

            List<AppSetting> updated = client.updateBulk(Map.of(
                    "company.name", "Magasin Central",
                    "app.currency", "XOF",
                    "stock.allow_negative", "true"
            ));
            assertTrue(updated.size() >= 3);
            Map<String, String> byKey = updated.stream()
                    .collect(java.util.stream.Collectors.toMap(AppSetting::key, AppSetting::value, (a, b) -> b));
            assertEquals("Magasin Central", byKey.get("company.name"));
            assertEquals("XOF", byKey.get("app.currency"));
            assertEquals("true", byKey.get("stock.allow_negative"));
        }
    }

    @Test
    void installationId() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            SettingsClient client = client(server);
            assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", client.installationId());
        }
    }

    @Test
    void withoutToken_401() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            SettingsClient client = new SettingsClient(new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2)));
            ApiException ex = assertThrows(ApiException.class, client::getAll);
            assertEquals(401, ex.statusCode());
        }
    }

    @Test
    void clientConfigAndLogo() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            SettingsClient client = client(server);
            var cfg = client.getClientConfig();
            assertEquals("Caisse 1", cfg.registerName());
            assertFalse(cfg.paymentMethods().isEmpty());

            var updated = client.updateClientConfig(Map.of(
                    "pos", Map.of(
                            "registerName", "Caisse 2",
                            "allowPartialPayment", true,
                            "allowSplitPayment", true
                    )
            ));
            assertTrue(updated.allowPartialPayment());

            var withLogo = client.uploadCompanyLogo("logo.png", new byte[] {1, 2, 3});
            assertTrue(withLogo.logoPath() != null && !withLogo.logoPath().isBlank());
        }
    }

    private static SettingsClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new SettingsClient(api);
    }
}
