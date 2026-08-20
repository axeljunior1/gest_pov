package com.erp.products.discovery;

import com.erp.products.config.DesktopProperties;
import com.erp.products.dto.DiscoveryResponse;
import com.erp.products.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServerDiscoveryService {

    public static final String APPLICATION = "GEST_POV";
    public static final String STATUS_READY = "READY";

    private final ServerIdentityService serverIdentityService;
    private final DesktopProperties desktopProperties;
    private final SettingsService settingsService;

    @Value("${server.port:8080}")
    private int serverPort;

    public DiscoveryResponse buildResponse() {
        return DiscoveryResponse.builder()
                .application(APPLICATION)
                .serverId(serverIdentityService.getServerId())
                .serverName(resolveServerName())
                .version(desktopProperties.getVersion())
                .status(STATUS_READY)
                .companyName(safeCompanyName())
                .port(serverPort)
                .build();
    }

    private String resolveServerName() {
        String configured = desktopProperties.getServerName();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "GEST-POV";
        }
    }

    private String safeCompanyName() {
        try {
            String name = settingsService.getPublicSettings().getCompanyName();
            return (name == null || name.isBlank()) ? null : name;
        } catch (Exception e) {
            log.debug("companyName indisponible pour discovery: {}", e.getMessage());
            return null;
        }
    }
}
