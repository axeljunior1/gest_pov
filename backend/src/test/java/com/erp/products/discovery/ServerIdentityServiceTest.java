package com.erp.products.discovery;

import com.erp.products.config.DesktopProperties;
import com.erp.products.config.LicenseProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ServerIdentityServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesOnceAndReusesFile() throws Exception {
        Path file = tempDir.resolve("server.id");
        DesktopProperties desktop = new DesktopProperties();
        desktop.setServerIdFile(file.toString());
        LicenseProperties license = new LicenseProperties();
        license.setDataDir(tempDir.toString());

        ServerIdentityService first = new ServerIdentityService(desktop, license);
        first.init();
        String id = first.getServerId();
        assertThat(id).isNotBlank();
        assertThat(Files.readString(file, StandardCharsets.UTF_8).trim()).isEqualTo(id);

        ServerIdentityService second = new ServerIdentityService(desktop, license);
        second.init();
        assertThat(second.getServerId()).isEqualTo(id);
    }

    @Test
    void fallsBackToLicenseDataDirWhenPathEmpty() {
        DesktopProperties desktop = new DesktopProperties();
        desktop.setServerIdFile("");
        LicenseProperties license = new LicenseProperties();
        license.setDataDir(tempDir.toString());

        ServerIdentityService service = new ServerIdentityService(desktop, license);
        service.init();
        assertThat(service.resolveFile()).isEqualTo(tempDir.toAbsolutePath().normalize().resolve("server.id"));
        assertThat(service.getServerId()).isNotBlank();
    }
}
