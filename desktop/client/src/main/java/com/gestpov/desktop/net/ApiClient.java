package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client HTTP unique. Token JWT en mémoire — jamais de mot de passe DB.
 * Indépendant de 127.0.0.1 vs IP LAN.
 */
public class ApiClient {

    private final ObjectMapper mapper;
    private final HttpClient http;
    private final Duration timeout;
    private String baseUrl;
    private String bearerToken;
    private Consumer<ApiException> unauthorizedHandler;

    public ApiClient(String host, int port, Duration timeout) {
        this("http://" + host + ":" + port, timeout, new ObjectMapper());
    }

    public ApiClient(String baseUrl, Duration timeout, ObjectMapper mapper) {
        this.baseUrl = trimSlash(baseUrl);
        this.timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
    }

    public void setUnauthorizedHandler(Consumer<ApiException> handler) {
        this.unauthorizedHandler = handler;
    }

    public void setBaseUrl(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
        this.bearerToken = null;
    }

    public void setBearerToken(String token) {
        this.bearerToken = token;
    }

    public void clearToken() {
        this.bearerToken = null;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public JsonNode get(String path) throws ApiException {
        return send(HttpRequest.newBuilder().uri(uri(path)).GET(), path);
    }

    public JsonNode get(String path, Map<String, String> query) throws ApiException {
        return send(HttpRequest.newBuilder().uri(uri(path, query)).GET(), path);
    }

    public JsonNode post(String path, Object body) throws ApiException {
        return sendJson(path, "POST", body);
    }

    public JsonNode put(String path, Object body) throws ApiException {
        return sendJson(path, "PUT", body);
    }

    public JsonNode patch(String path, Object body) throws ApiException {
        return sendJson(path, "PATCH", body);
    }

    public JsonNode delete(String path) throws ApiException {
        return send(HttpRequest.newBuilder().uri(uri(path)).DELETE(), path);
    }

    public JsonNode postMultipart(String path, String fieldName, String fileName, byte[] fileBytes,
                                  Map<String, String> fields) throws ApiException {
        String boundary = "GestPovBoundary" + System.nanoTime();
        byte[] payload = multipartBody(boundary, fieldName, fileName, fileBytes, fields);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload));
        return send(builder, path);
    }

    private JsonNode sendJson(String path, String method, Object body) throws ApiException {
        try {
            String json = mapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri(path))
                    .header("Content-Type", "application/json");
            if ("PUT".equals(method)) {
                builder.PUT(HttpRequest.BodyPublishers.ofString(json));
            } else if ("PATCH".equals(method)) {
                builder.method("PATCH", HttpRequest.BodyPublishers.ofString(json));
            } else {
                builder.POST(HttpRequest.BodyPublishers.ofString(json));
            }
            return send(builder, path);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Impossible de sérialiser la requête", e);
        }
    }

    private JsonNode send(HttpRequest.Builder builder, String path) throws ApiException {
        builder.timeout(timeout);
        builder.header("Accept", "application/json");
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body() == null ? "" : response.body();
            if (status >= 200 && status < 300) {
                return parseSuccess(body, status);
            }
            ApiException error = new ApiException(extractMessage(body, status), status, body);
            if (status == 401 && !isLoginPath(path) && unauthorizedHandler != null) {
                unauthorizedHandler.accept(error);
            }
            throw error;
        } catch (ApiException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            throw new ApiException("Délai dépassé", 0, null, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Requête interrompue", e);
        } catch (Exception e) {
            throw new ApiException("Serveur indisponible", e);
        }
    }

    private JsonNode parseSuccess(String body, int status) throws ApiException {
        if (body.isBlank()) {
            return mapper.createObjectNode();
        }
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw new ApiException("Réponse serveur illisible", status, body, e);
        }
    }

    private URI uri(String path) {
        return URI.create(baseUrl + suffix(path));
    }

    private URI uri(String path, Map<String, String> query) {
        StringBuilder sb = new StringBuilder(baseUrl).append(suffix(path));
        if (query != null && !query.isEmpty()) {
            sb.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) {
                    sb.append('&');
                }
                first = false;
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        return URI.create(sb.toString());
    }

    private static String suffix(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private static boolean isLoginPath(String path) {
        return path != null && path.contains("/api/auth/login");
    }

    String extractMessage(String body, int status) {
        try {
            JsonNode node = mapper.readTree(body);
            if (node.hasNonNull("message") && !node.get("message").asText().isBlank()) {
                return node.get("message").asText();
            }
            JsonNode errors = node.get("errors");
            if (errors != null && errors.isObject() && errors.hasNonNull("nom")) {
                return errors.get("nom").asText();
            }
        } catch (Exception ignored) {
            // corps non JSON
        }
        return "Erreur HTTP " + status;
    }

    private static byte[] multipartBody(String boundary, String fieldName, String fileName, byte[] fileBytes,
                                        Map<String, String> fields) {
        String dash = "--" + boundary + "\r\n";
        StringBuilder head = new StringBuilder();
        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                head.append(dash)
                        .append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n\r\n")
                        .append(entry.getValue() == null ? "" : entry.getValue())
                        .append("\r\n");
            }
        }
        head.append(dash)
                .append("Content-Disposition: form-data; name=\"").append(fieldName)
                .append("\"; filename=\"").append(fileName == null ? "file" : fileName).append("\"\r\n")
                .append("Content-Type: application/octet-stream\r\n\r\n");
        byte[] header = head.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] content = fileBytes == null ? new byte[0] : fileBytes;
        byte[] all = new byte[header.length + content.length + footer.length];
        System.arraycopy(header, 0, all, 0, header.length);
        System.arraycopy(content, 0, all, header.length, content.length);
        System.arraycopy(footer, 0, all, header.length + content.length, footer.length);
        return all;
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "http://127.0.0.1:8080";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public Map<String, Object> loginPayload(String email, String password) {
        return Map.of("email", email, "password", password);
    }
}
