package com.gestpov.desktop.net;

import com.gestpov.desktop.model.AppSetting;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettingsClientTest {

    @Test
    void getAllAndUpdate() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            SettingsClient client = client(server);
            List<AppSetting> all = client.getAll();
            assertEquals(2, all.size());
            assertEquals("Gest POV Test", all.get(0).value());

            AppSetting updated = client.update("company.name", "Boutique Nord");
            assertEquals("Boutique Nord", updated.value());
            assertEquals("Boutique Nord", client.getAll().get(0).value());
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

    private static SettingsClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new SettingsClient(api);
    }
}
