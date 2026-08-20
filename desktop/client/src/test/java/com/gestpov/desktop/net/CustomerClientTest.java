package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Customer;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerClientTest {

    @Test
    void listSearchCreateUpdateDelete() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CustomerClient client = client(server);
            List<Customer> all = client.list();
            assertEquals(1, all.size());
            assertEquals("Marie", all.get(0).firstName());

            Customer created = client.create(new Customer(null, "Jean", "Martin", "0700000000",
                    "jean@test.local", "", "", "", 0));
            assertEquals("Jean Martin", created.displayName());
            assertEquals(2, client.list().size());
            assertEquals(1, client.search("marie").size());

            Customer updated = client.update(created.id(), new Customer(created.id(), "Jean", "Martin",
                    "0700000001", "jean@test.local", "", "", "", 0));
            assertEquals("0700000001", updated.phone());

            client.delete(created.id());
            assertEquals(1, client.list().size());
        }
    }

    @Test
    void withoutToken_401() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            CustomerClient client = new CustomerClient(new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2)));
            ApiException ex = assertThrows(ApiException.class, client::list);
            assertEquals(401, ex.statusCode());
        }
    }

    private static CustomerClient client(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return new CustomerClient(api);
    }
}
