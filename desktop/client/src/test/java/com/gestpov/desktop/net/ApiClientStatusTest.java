package com.gestpov.desktop.net;

import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiClientStatusTest {

    @Test
    void getOk() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = authenticated(server);
            var node = api.get("/api/brands");
            assertTrue(node.isArray());
            assertTrue(node.size() >= 1);
        }
    }

    @Test
    void unauthorized401_triggersHandler() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            AtomicBoolean expired = new AtomicBoolean(false);
            api.setUnauthorizedHandler(e -> expired.set(true));
            ApiException ex = assertThrows(ApiException.class, () -> api.get("/api/status/401"));
            assertEquals(401, ex.statusCode());
            assertTrue(expired.get());
            assertEquals("Votre session a expiré. Veuillez vous reconnecter.", ApiException.userMessage(ex));
        }
    }

    @Test
    void forbidden403() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            ApiException ex = assertThrows(ApiException.class, () -> api.get("/api/status/403"));
            assertEquals(403, ex.statusCode());
            assertEquals("Vous n'avez pas l'autorisation d'effectuer cette opération.", ApiException.userMessage(ex));
        }
    }

    @Test
    void serverError500_hidesTechnicalMessage() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            ApiException ex = assertThrows(ApiException.class, () -> api.get("/api/status/500"));
            assertEquals(500, ex.statusCode());
            assertEquals("Une erreur technique est survenue. Réessayez dans quelques instants.", ApiException.userMessage(ex));
            assertFalse(ApiException.userMessage(ex).toLowerCase().contains("boom"));
        }
    }

    @Test
    void invalidJson() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            ApiException ex = assertThrows(ApiException.class, () -> api.get("/api/status/invalid-json"));
            assertTrue(ex.getMessage().toLowerCase().contains("illisible"));
            assertFalse(ApiException.userMessage(ex).contains("<<<"));
        }
    }

    @Test
    void timeout() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            server.sleepMs = 1500;
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofMillis(200));
            ApiException ex = assertThrows(ApiException.class, () -> api.get("/api/discovery"));
            assertTrue(ex.isTimeout() || ex.isNetwork());
        }
    }

    @Test
    void loginDoesNotFireUnauthorizedHandler() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            AtomicBoolean expired = new AtomicBoolean(false);
            api.setUnauthorizedHandler(e -> expired.set(true));
            AuthClient auth = new AuthClient(api);
            assertThrows(ApiException.class, () -> auth.login("admin@erp.local", "wrong"));
            assertFalse(expired.get());
        }
    }

    private static ApiClient authenticated(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return api;
    }
}
