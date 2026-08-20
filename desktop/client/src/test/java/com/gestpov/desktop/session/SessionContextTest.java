package com.gestpov.desktop.session;

import com.gestpov.desktop.net.ApiClient;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.AuthClient;
import com.gestpov.desktop.net.AuthSession;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionContextTest {

    @Test
    void tokenAndPermissionsAfterLogin() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            SessionContext session = new SessionContext(api);
            session.bindServer(server.serverId, "TEST", server.version);
            AuthSession user = new AuthClient(api).login(server.validEmail, server.validPassword);
            session.setUser(user);

            assertTrue(session.isAuthenticated());
            assertEquals("jwt-test-token", api.getBearerToken());
            assertTrue(session.hasPermission("products.read"));
            assertFalse(session.hasPermission("roles.delete"));
            assertEquals(server.serverId, session.serverId());
            assertEquals("1.0.0", session.serverVersion());
        }
    }

    @Test
    void logoutClearsTokenAndUser() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            SessionContext session = new SessionContext(api);
            session.setUser(new AuthClient(api).login(server.validEmail, server.validPassword));
            session.clear();
            assertFalse(session.isAuthenticated());
            assertNull(api.getBearerToken());
            assertNull(session.user());
        }
    }

    @Test
    void http401ClearsSession() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
            SessionContext session = new SessionContext(api);
            session.setUser(new AuthClient(api).login(server.validEmail, server.validPassword));
            AtomicBoolean expired = new AtomicBoolean(false);
            session.onSessionExpired(() -> expired.set(true));
            assertThrows(ApiException.class, () -> api.get("/api/status/401"));
            assertTrue(expired.get());
            assertFalse(session.isAuthenticated());
        }
    }
}
