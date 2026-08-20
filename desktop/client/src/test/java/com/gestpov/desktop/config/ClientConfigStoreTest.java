package com.gestpov.desktop.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientConfigStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void load_returnsDefaultsWhenFileMissing() {
        ClientConfigStore store = new ClientConfigStore(tempDir.resolve("missing.properties"));
        ClientConfig config = store.load();
        assertEquals(8080, config.port());
        assertEquals("", config.serverId());
        assertEquals(ClientConfig.CURRENT_VERSION, config.clientVersion());
    }

    @Test
    void save_thenLoad_roundTrip() throws Exception {
        Path file = tempDir.resolve("client.properties");
        ClientConfigStore store = new ClientConfigStore(file);
        ClientConfig saved = ClientConfig.empty()
                .withServer("abc-id", "192.168.1.10", 8080, "CAISSE")
                .withLastLoginEmail("admin@gestpov.local");
        store.save(saved);

        ClientConfig loaded = store.load();
        assertEquals("abc-id", loaded.serverId());
        assertEquals("192.168.1.10", loaded.host());
        assertEquals(8080, loaded.port());
        assertEquals("CAISSE", loaded.serverName());
        assertEquals("admin@gestpov.local", loaded.lastLoginEmail());
        assertTrue(loaded.hasRememberedServer());
    }
}
