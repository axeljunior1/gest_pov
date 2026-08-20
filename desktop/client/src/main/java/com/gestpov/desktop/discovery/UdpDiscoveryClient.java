package com.gestpov.desktop.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sonde UDP. Les réponses ne sont que des candidats — validation HTTP obligatoire ensuite.
 */
public class UdpDiscoveryClient {

    public static final String PROBE = "GEST_POV_DISCOVERY";

    private final ObjectMapper mapper;
    private final Duration timeout;

    public UdpDiscoveryClient(Duration timeout) {
        this(timeout, new ObjectMapper());
    }

    public UdpDiscoveryClient(Duration timeout, ObjectMapper mapper) {
        this.timeout = timeout == null ? Duration.ofMillis(1500) : timeout;
        this.mapper = mapper;
    }

    public List<UdpCandidate> probe(int udpPort) {
        Map<String, UdpCandidate> found = new LinkedHashMap<>();
        byte[] probe = PROBE.getBytes(StandardCharsets.UTF_8);
        try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(0))) {
            socket.setBroadcast(true);
            socket.setSoTimeout((int) Math.max(200, timeout.toMillis()));
            for (InetAddress target : probeTargets()) {
                try {
                    DatagramPacket packet = new DatagramPacket(probe, probe.length, target, udpPort);
                    socket.send(packet);
                } catch (Exception ignored) {
                    // interface non routable
                }
            }
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            byte[] buf = new byte[4096];
            while (System.currentTimeMillis() < deadline) {
                DatagramPacket incoming = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(incoming);
                } catch (SocketTimeoutException e) {
                    break;
                }
                String host = incoming.getAddress().getHostAddress();
                int httpPort = 8080;
                try {
                    JsonNode json = mapper.readTree(new String(
                            incoming.getData(), incoming.getOffset(), incoming.getLength(), StandardCharsets.UTF_8));
                    if (json.has("port")) {
                        httpPort = json.path("port").asInt(8080);
                    }
                } catch (Exception ignored) {
                    // JSON invalide : on tente quand même HTTP sur 8080
                }
                found.put(host + ":" + httpPort, new UdpCandidate(host, httpPort));
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return new ArrayList<>(found.values());
    }

    private static List<InetAddress> probeTargets() {
        List<InetAddress> targets = new ArrayList<>();
        try {
            targets.add(InetAddress.getByName("127.0.0.1"));
            targets.add(InetAddress.getByName("255.255.255.255"));
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces != null) {
                for (NetworkInterface nif : Collections.list(ifaces)) {
                    if (!nif.isUp() || nif.isLoopback()) {
                        continue;
                    }
                    nif.getInterfaceAddresses().forEach(addr -> {
                        if (addr.getBroadcast() != null) {
                            targets.add(addr.getBroadcast());
                        }
                    });
                }
            }
        } catch (Exception ignored) {
            // 127.0.0.1 suffit en local
        }
        return targets;
    }

    public record UdpCandidate(String host, int httpPort) {}
}
