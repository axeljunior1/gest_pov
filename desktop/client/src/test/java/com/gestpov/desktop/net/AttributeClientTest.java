package com.gestpov.desktop.net;

import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeClientTest {

    @Test
    void createListDelete() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            AttributeClient client = client(server);
            assertTrue(client.findAll().isEmpty());
            var created = client.create("COLOR", "Couleur", "TEXT");
            assertEquals("COLOR", created.code());
            assertEquals(1, client.findAll().size());
            client.delete(created.id());
            assertTrue(client.findAll().isEmpty());
        }
    }

    private static AttributeClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new AttributeClient(api);
    }
}
