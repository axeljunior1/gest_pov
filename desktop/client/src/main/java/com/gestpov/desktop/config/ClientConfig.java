package com.gestpov.desktop.config;

/**
 * Configuration client Desktop — jamais de secrets PostgreSQL ni mot de passe utilisateur.
 */
public record ClientConfig(
        String serverId,
        String host,
        int port,
        String serverName,
        String hostnameFallback,
        String clientVersion,
        int discoveryUdpPort,
        int timeoutMs,
        String lastLoginEmail,
        String ticketPrinterName,
        int ticketPaperWidthChars
) {
    public static final String CURRENT_VERSION = "1.0.0";
    /** 80mm ≈ 48 caracteres en police ticket standard. */
    public static final int DEFAULT_TICKET_WIDTH_CHARS = 48;

    public static ClientConfig empty() {
        return new ClientConfig("", "", 8080, "", "", CURRENT_VERSION, 38471, 20000, "", "",
                DEFAULT_TICKET_WIDTH_CHARS);
    }

    public ClientConfig withServer(String id, String newHost, int newPort, String name) {
        return new ClientConfig(
                id == null ? "" : id,
                newHost == null ? "" : newHost,
                newPort,
                name == null ? "" : name,
                hostnameFallback,
                clientVersion,
                discoveryUdpPort,
                timeoutMs,
                lastLoginEmail,
                ticketPrinterName,
                ticketPaperWidthChars
        );
    }

    public ClientConfig withLastLoginEmail(String email) {
        return new ClientConfig(
                serverId,
                host,
                port,
                serverName,
                hostnameFallback,
                clientVersion,
                discoveryUdpPort,
                timeoutMs,
                email == null ? "" : email,
                ticketPrinterName,
                ticketPaperWidthChars
        );
    }

    /** Imprimante ticket locale a ce poste (nom vide = pas d'imprimante ESC/POS configuree). */
    public ClientConfig withTicketPrinter(String printerName, int paperWidthChars) {
        return new ClientConfig(
                serverId,
                host,
                port,
                serverName,
                hostnameFallback,
                clientVersion,
                discoveryUdpPort,
                timeoutMs,
                lastLoginEmail,
                printerName == null ? "" : printerName,
                paperWidthChars > 0 ? paperWidthChars : DEFAULT_TICKET_WIDTH_CHARS
        );
    }

    public boolean hasTicketPrinter() {
        return ticketPrinterName != null && !ticketPrinterName.isBlank();
    }

    public boolean hasRememberedServer() {
        return host != null && !host.isBlank() && port > 0;
    }
}
