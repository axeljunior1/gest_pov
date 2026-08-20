package com.gestpov.desktop.net;

import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiClientAuthTest {

    @Test
    void loginOk_thenAuthenticatedMe() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            AuthClient auth = new AuthClient(api);
            AuthSession session = auth.login("admin@erp.local", "ErpTestPass-OK");
            assertEquals("jwt-test-token", session.token());
            AuthSession me = auth.me();
            assertEquals("admin@erp.local", me.email());
            assertEquals("Admin ERP", me.displayName());
        }
    }

    @Test
    void loginKo_invalidCredentials() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            AuthClient auth = new AuthClient(api);
            ApiException ex = assertThrows(ApiException.class, () -> auth.login("admin@erp.local", "wrong"));
            assertEquals(400, ex.statusCode());
            assertTrue(ex.getMessage().toLowerCase().contains("incorrect"));
        }
    }

    @Test
    void timeout_whenServerTooSlow() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            server.sleepMs = 1500;
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofMillis(200));
            AuthClient auth = new AuthClient(api);
            ApiException ex = assertThrows(ApiException.class, () -> auth.login("admin@erp.local", "secret"));
            assertTrue(ex.isTimeout() || ex.isNetwork());
        }
    }

    @Test
    void me_withoutToken_fails() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            AuthClient auth = new AuthClient(api);
            ApiException ex = assertThrows(ApiException.class, auth::me);
            assertEquals(400, ex.statusCode());
        }
    }
}
