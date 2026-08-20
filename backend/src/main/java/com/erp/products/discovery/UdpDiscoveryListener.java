package com.erp.products.discovery;

import com.erp.products.config.DesktopProperties;
import com.erp.products.dto.DiscoveryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Répond aux sondes UDP LAN. Activé uniquement si app.desktop.discovery.enabled=true
 * (profils dev et desktop). Inactif en Docker/prod par défaut.
 */
@Component
@ConditionalOnProperty(prefix = "app.desktop.discovery", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class UdpDiscoveryListener {

    private final DesktopProperties desktopProperties;
    private final ServerDiscoveryService serverDiscoveryService;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;
    private DatagramSocket socket;

    @PostConstruct
    void start() {
        running.set(true);
        worker = new Thread(this::listenLoop, "gest-pov-udp-discovery");
        worker.setDaemon(true);
        worker.start();
        log.info("Discovery UDP activée sur le port {}", desktopProperties.getDiscovery().getUdpPort());
    }

    @PreDestroy
    void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void listenLoop() {
        int port = desktopProperties.getDiscovery().getUdpPort();
        String probe = desktopProperties.getDiscovery().getProbe();
        try (DatagramSocket ds = new DatagramSocket(port)) {
            this.socket = ds;
            ds.setBroadcast(true);
            byte[] buf = new byte[2048];
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                DatagramPacket incoming = new DatagramPacket(buf, buf.length);
                try {
                    ds.receive(incoming);
                    String message = new String(incoming.getData(), incoming.getOffset(), incoming.getLength(), StandardCharsets.UTF_8).trim();
                    if (!probe.equals(message)) {
                        continue;
                    }
                    DiscoveryResponse payload = serverDiscoveryService.buildResponse();
                    byte[] out = objectMapper.writeValueAsBytes(payload);
                    DatagramPacket reply = new DatagramPacket(
                            out, out.length, incoming.getAddress(), incoming.getPort());
                    ds.send(reply);
                } catch (Exception e) {
                    if (running.get()) {
                        log.debug("Sonde UDP ignorée: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                log.warn("Impossible d'écouter la discovery UDP ({}): {}", port, e.getMessage());
            }
        }
    }
}
