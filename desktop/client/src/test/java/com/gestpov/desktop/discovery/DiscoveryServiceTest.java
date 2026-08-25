package com.gestpov.desktop.discovery;

import com.gestpov.desktop.config.ClientConfig;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.support.FakeHttpServer;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryServiceTest {

    @Test
    void rememberedServer_available() throws Exception {
        try (FakeHttpServer http = new FakeHttpServer()) {
            ClientConfig config = httpOnlyConfig(http.serverId, "127.0.0.1", http.port(), "TEST");
            DiscoveryService discovery = httpDiscovery();
            List<DiscoveredServer> found = discovery.findServers(config);
            assertEquals(1, found.size());
            assertEquals(http.serverId, found.get(0).serverId());
            assertTrue(found.get(0).isGestPov());
        }
    }

    @Test
    void rememberedServer_unavailable() {
        ClientConfig config = httpOnlyConfig("abc", "127.0.0.1", 1, "DOWN");
        DiscoveryService discovery = new DiscoveryService(
                new UdpDiscoveryClient(Duration.ofMillis(100)),
                Duration.ofMillis(200),
                "1.0.0");
        assertTrue(discovery.findServers(config).isEmpty());
    }

    @Test
    void wrongApplication_isRejected() throws Exception {
        try (FakeHttpServer http = new FakeHttpServer()) {
            http.application = "OTHER_APP";
            DiscoveryService discovery = httpDiscovery();
            assertThrows(ApiException.class, () -> discovery.validateHttp("127.0.0.1", http.port()));
            ClientConfig config = httpOnlyConfig("x", "127.0.0.1", http.port(), "");
            assertTrue(discovery.findServers(config).isEmpty());
        }
    }

    @Test
    void udpCandidate_requiresHttpValidation() throws Exception {
        try (FakeHttpServer http = new FakeHttpServer();
             DatagramSocket udp = new DatagramSocket(0)) {
            startUdpEcho(udp, http.port());
            ClientConfig config = new ClientConfig(
                    "", "", 8080, "", "", "1.0.0", udp.getLocalPort(), 2000, "", "", 48);
            DiscoveryService discovery = new DiscoveryService(
                    new UdpDiscoveryClient(Duration.ofMillis(800)),
                    Duration.ofSeconds(2),
                    "1.0.0");
            List<DiscoveredServer> found = discovery.findServers(config);
            assertEquals(1, found.size());
            assertEquals(http.serverId, found.get(0).serverId());
            assertEquals(http.port(), found.get(0).port());
        }
    }

    @Test
    void sameServerId_afterIpChange_isFound() throws Exception {
        try (FakeHttpServer http = new FakeHttpServer();
             DatagramSocket udp = new DatagramSocket(0)) {
            startUdpEcho(udp, http.port());
            ClientConfig oldIp = new ClientConfig(
                    http.serverId, "10.255.255.1", 1, "OLD-IP", "", "1.0.0", udp.getLocalPort(), 500, "", "", 48);
            DiscoveryService discovery = new DiscoveryService(
                    new UdpDiscoveryClient(Duration.ofMillis(800)),
                    Duration.ofSeconds(2),
                    "1.0.0");
            List<DiscoveredServer> found = discovery.findServers(oldIp);
            assertEquals(1, found.size());
            assertEquals(http.serverId, found.get(0).serverId());
            assertEquals("127.0.0.1", found.get(0).host());
        }
    }

    private static DiscoveryService httpDiscovery() {
        return new DiscoveryService(
                new UdpDiscoveryClient(Duration.ofMillis(100)),
                Duration.ofSeconds(2),
                "1.0.0");
    }

    /** Port UDP libre : les tests HTTP-only ne doivent pas sonder le backend réel (38471). */
    private static ClientConfig httpOnlyConfig(String serverId, String host, int port, String name) {
        return new ClientConfig(serverId, host, port, name, "", "1.0.0", unusedUdpPort(), 500, "", "", 48);
    }

    private static int unusedUdpPort() {
        try (DatagramSocket socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            return 61999;
        }
    }

    private static void startUdpEcho(DatagramSocket udp, int httpPort) {
        Thread listener = new Thread(() -> {
            try {
                byte[] buf = new byte[512];
                DatagramPacket incoming = new DatagramPacket(buf, buf.length);
                udp.setSoTimeout(2500);
                udp.receive(incoming);
                byte[] json = ("{\"application\":\"GEST_POV\",\"port\":" + httpPort + "}")
                        .getBytes(StandardCharsets.UTF_8);
                udp.send(new DatagramPacket(json, json.length, incoming.getAddress(), incoming.getPort()));
            } catch (Exception ignored) {
                // fin de test
            }
        }, "udp-echo");
        listener.setDaemon(true);
        listener.start();
    }
}
