package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Alert;
import com.gestpov.desktop.model.LicenseStatus;
import com.gestpov.desktop.model.Role;
import com.gestpov.desktop.model.SaleBrowsePage;
import com.gestpov.desktop.model.UserAccount;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseEfgClientsTest {

    @Test
    void usersRolesAlerts() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = authed(server);
            UserClient users = new UserClient(api);
            RoleClient roles = new RoleClient(api);
            AlertClient alerts = new AlertClient(api);

            List<UserAccount> list = users.list();
            assertEquals(1, list.size());
            assertEquals("Admin", list.get(0).firstName());

            UserAccount created = users.create("Jean", "Test", "jean@test.local", "Secret1!", null, null, true,
                    List.of(2L));
            assertEquals("Jean", created.firstName());

            List<Role> roleList = roles.list();
            assertEquals(2, roleList.size());
            assertFalse(roles.listPermissions().isEmpty());
            Role updated = roles.updatePermissions(1L, List.of("users.read", "roles.read"));
            assertNotNull(updated);

            List<Alert> open = alerts.list(null, "OPEN");
            assertEquals(1, open.size());
            assertEquals("ACKNOWLEDGED", alerts.acknowledge(1L).status());
        }
    }

    @Test
    void licenseImportExportDashboardSalesAnalytics() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer()) {
            ApiClient api = authed(server);
            LicenseClient license = new LicenseClient(api);
            ImportExportClient io = new ImportExportClient(api);
            DashboardClient dash = new DashboardClient(api);
            SalesBrowseClient sales = new SalesBrowseClient(api);
            AnalyticsClient analytics = new AnalyticsClient(api);

            LicenseStatus status = license.status();
            assertFalse(status.valid());
            LicenseStatus imported = license.importLicense("demo.lic", "lic".getBytes(StandardCharsets.UTF_8));
            assertTrue(imported.valid());

            assertTrue(io.productTemplate("CSV").length > 0);
            assertEquals(3, io.previewProducts("p.csv", "a,b".getBytes(StandardCharsets.UTF_8), "REJECT").totalRows());
            assertEquals(1, io.history().size());
            assertTrue(io.exportProducts("CSV").length > 0);

            assertEquals(12, dash.summary().totalProducts());
            assertEquals(3, dash.alerts().openAlerts());
            assertEquals("UP", dash.health().path("status").asText());

            SaleBrowsePage page = sales.browse(null, null, 0, 50);
            assertEquals(1, page.items().size());
            assertNotNull(sales.detail(1L).sale());

            assertEquals("EUR", analytics.overview().currency());
            assertEquals(1, analytics.cancelledSales().size());
        }
    }

    private static ApiClient authed(FakeHttpServer server) throws ApiException {
        ApiClient api = new ApiClient("127.0.0.1", server.port(), Duration.ofSeconds(2));
        new AuthClient(api).login(server.validEmail, server.validPassword);
        return api;
    }
}
