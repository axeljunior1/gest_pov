package com.gestpov.desktop.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class FakeHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final AtomicInteger loginHits = new AtomicInteger();
    private final AtomicLong nextBrandId = new AtomicLong(1);
    private final AtomicLong nextCategoryId = new AtomicLong(1);
    private final AtomicLong nextProductId = new AtomicLong(1);
    private final AtomicLong nextSupplierId = new AtomicLong(1);
    private final AtomicLong nextUnitId = new AtomicLong(1);
    private final AtomicLong nextImageId = new AtomicLong(1);
    private final AtomicLong nextHistoryId = new AtomicLong(1);
    private final AtomicLong nextCustomerId = new AtomicLong(1);
    private final AtomicLong nextStockItemId = new AtomicLong(1);
    private final AtomicLong nextSaleId = new AtomicLong(1);
    private final AtomicLong nextSaleLineId = new AtomicLong(1);
    private final List<BrandRow> brands = new ArrayList<>();
    private final List<CategoryRow> categories = new ArrayList<>();
    private final List<ProductRow> products = new ArrayList<>();
    private final List<SupplierRow> suppliers = new ArrayList<>();
    private final List<UnitRow> units = new ArrayList<>();
    private final List<PriceHistoryRow> priceHistories = new ArrayList<>();
    private final List<CustomerRow> customers = new ArrayList<>();
    private final List<StockItemRow> stockItems = new ArrayList<>();
    private final List<SettingRow> settings = new ArrayList<>();
    private final List<SaleRow> sales = new ArrayList<>();
    private boolean posSessionOpen = false;

    public String application = "GEST_POV";
    public String serverId = "11111111-1111-1111-1111-111111111111";
    public String version = "1.0.0";
    public String validEmail = "admin@erp.local";
    public String validPassword = "ErpTestPass-OK";
    public int sleepMs = 0;
    public int brandsForcedStatus = 0;
    public boolean brandsInvalidJson = false;
    public String brandsForcedMessage = "forced";

    public FakeHttpServer() throws IOException {
        brands.add(new BrandRow(nextBrandId.getAndIncrement(), "Nike"));
        CategoryRow electronics = new CategoryRow(nextCategoryId.getAndIncrement(), "Électronique", null);
        categories.add(electronics);
        categories.add(new CategoryRow(nextCategoryId.getAndIncrement(), "Téléphones", electronics.id));
        suppliers.add(new SupplierRow(nextSupplierId.getAndIncrement(), "Grossiste Nord"));
        units.add(new UnitRow(nextUnitId.getAndIncrement(), "Pièce", "pce"));
        ProductRow cahier = new ProductRow(nextProductId.getAndIncrement(), "Cahier A4");
        cahier.sku = "CAH-001";
        cahier.categorieId = electronics.id;
        cahier.categorieNom = electronics.nom;
        cahier.marqueId = 1L;
        cahier.marque = "Nike";
        cahier.prixVente = "3.50";
        cahier.statut = "ACTIF";
        cahier.cycleVie = "BROUILLON";
        cahier.stockTotal = 10;
        cahier.unitId = 1L;
        cahier.unitSymbole = "pce";
        cahier.fournisseurId = 1L;
        cahier.fournisseurNom = "Grossiste Nord";
        products.add(cahier);
        customers.add(new CustomerRow(nextCustomerId.getAndIncrement(), "Marie", "Dupont", "0600000000", "marie@test.local"));
        stockItems.add(new StockItemRow(nextStockItemId.getAndIncrement(), cahier.id, cahier.nom, "PRINCIPAL", "A-01",
                cahier.unitSymbole, "10", "10"));
        settings.add(new SettingRow("company.name", "Gest POV Test", "Nom de l'entreprise", "STRING"));
        settings.add(new SettingRow("pos.register.name", "Caisse 1", "Nom de caisse", "STRING"));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/discovery", exchange -> {
            sleep();
            json(exchange, 200, """
                    {"application":"%s","serverId":"%s","serverName":"TEST","version":"%s","status":"READY","port":%d}
                    """.formatted(application, serverId, version, port()));
        });
        server.createContext("/api/auth/login", exchange -> {
            sleep();
            loginHits.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean ok = body.contains(validEmail) && body.contains(validPassword);
            if (ok) {
                json(exchange, 200, """
                        {"token":"jwt-test-token","tokenType":"Bearer","user":{"email":"%s","firstName":"Admin","lastName":"ERP","roles":["ADMIN"]},"permissions":%s}
                        """.formatted(validEmail, permissionsJson()));
            } else {
                json(exchange, 400, "{\"message\":\"Email ou mot de passe incorrect\"}");
            }
        });
        server.createContext("/api/auth/me", exchange -> {
            sleep();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.contains("jwt-test-token")) {
                json(exchange, 400, "{\"message\":\"Non authentifie\"}");
                return;
            }
            json(exchange, 200, """
                    {"email":"%s","firstName":"Admin","lastName":"ERP","roles":["ADMIN"],"permissions":%s}
                    """.formatted(validEmail, permissionsJson()));
        });
        server.createContext("/api/status/401", exchange -> json(exchange, 401, "{\"message\":\"Authentification requise\"}"));
        server.createContext("/api/status/403", exchange -> json(exchange, 403, "{\"message\":\"Acces refuse — permission insuffisante\"}"));
        server.createContext("/api/status/500", exchange -> json(exchange, 500, "{\"message\":\"boom\"}"));
        server.createContext("/api/status/invalid-json", exchange -> text(exchange, 200, "<<<not-json>>>"));
        server.createContext("/api/brands", this::handleBrands);
        server.createContext("/api/categories", this::handleCategories);
        server.createContext("/api/products", this::handleProducts);
        server.createContext("/api/suppliers", this::handleSuppliers);
        server.createContext("/api/units", this::handleUnits);
        server.createContext("/api/customers", this::handleCustomers);
        server.createContext("/api/stock", this::handleStock);
        server.createContext("/api/settings", this::handleSettings);
        server.createContext("/api/pos", this::handlePos);
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public int loginHits() {
        return loginHits.get();
    }

    public List<BrandRow> brands() {
        return brands;
    }

    public List<CategoryRow> categories() {
        return categories;
    }

    private void handleBrands(HttpExchange exchange) throws IOException {
        sleep();
        if (brandsForcedStatus > 0) {
            json(exchange, brandsForcedStatus, "{\"message\":\"" + brandsForcedMessage + "\"}");
            return;
        }
        if (brandsInvalidJson) {
            text(exchange, 200, "not-json");
            return;
        }
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.contains("jwt-test-token")) {
            json(exchange, 401, "{\"message\":\"Authentification requise\"}");
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();
        if ("GET".equals(method) && path.equals("/api/brands/search")) {
            String nom = queryParam(query, "nom");
            json(exchange, 200, toArray(brands.stream()
                    .filter(b -> b.nom.toLowerCase().contains(nom.toLowerCase()))
                    .toList()));
            return;
        }
        if ("GET".equals(method) && path.equals("/api/brands")) {
            json(exchange, 200, toArray(brands));
            return;
        }
        if ("POST".equals(method) && path.equals("/api/brands")) {
            String nom = extractNom(exchange);
            if (nom.isBlank()) {
                json(exchange, 400, "{\"errors\":{\"nom\":\"Le nom est obligatoire\"}}");
                return;
            }
            if (brands.stream().anyMatch(b -> b.nom.equalsIgnoreCase(nom))) {
                json(exchange, 400, "{\"message\":\"Une marque avec ce nom existe déjà\"}");
                return;
            }
            BrandRow created = new BrandRow(nextBrandId.getAndIncrement(), nom);
            brands.add(created);
            json(exchange, 201, created.json());
            return;
        }
        if ("PUT".equals(method) && path.startsWith("/api/brands/")) {
            Long id = parseId(path);
            String nom = extractNom(exchange);
            BrandRow existing = find(id);
            if (existing == null) {
                json(exchange, 404, "{\"message\":\"Marque non trouvée: " + id + "\"}");
                return;
            }
            if (brands.stream().anyMatch(b -> !b.id.equals(id) && b.nom.equalsIgnoreCase(nom))) {
                json(exchange, 400, "{\"message\":\"Une marque avec ce nom existe déjà\"}");
                return;
            }
            existing.nom = nom;
            json(exchange, 200, existing.json());
            return;
        }
        if ("DELETE".equals(method) && path.startsWith("/api/brands/")) {
            Long id = parseId(path);
            BrandRow existing = find(id);
            if (existing == null) {
                json(exchange, 404, "{\"message\":\"Marque non trouvée: " + id + "\"}");
                return;
            }
            if ("Linked".equalsIgnoreCase(existing.nom)) {
                json(exchange, 400, "{\"message\":\"Impossible : cette marque est liée à des produits\"}");
                return;
            }
            brands.remove(existing);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        json(exchange, 404, "{\"message\":\"not found\"}");
    }

    private void handleCategories(HttpExchange exchange) throws IOException {
        sleep();
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.contains("jwt-test-token")) {
            json(exchange, 401, "{\"message\":\"Authentification requise\"}");
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();
        if ("GET".equals(method) && path.equals("/api/categories/search")) {
            String nom = queryParam(query, "nom");
            json(exchange, 200, toCategoryArray(categories.stream()
                    .filter(c -> c.nom.toLowerCase().contains(nom.toLowerCase()))
                    .toList(), false));
            return;
        }
        if ("GET".equals(method) && path.equals("/api/categories")) {
            json(exchange, 200, toCategoryArray(categories.stream()
                    .filter(c -> c.parentId == null)
                    .toList(), true));
            return;
        }
        if ("GET".equals(method) && path.startsWith("/api/categories/")) {
            CategoryRow existing = findCategory(parseId(path));
            if (existing == null) {
                json(exchange, 404, "{\"message\":\"Catégorie non trouvée: " + parseId(path) + "\"}");
                return;
            }
            json(exchange, 200, categoryJson(existing, true));
            return;
        }
        if ("POST".equals(method) && path.equals("/api/categories")) {
            String body = readBody(exchange);
            String nom = extractNomFrom(body);
            Long parentId = extractLongField(body, "parentId");
            if (nom.isBlank()) {
                json(exchange, 400, "{\"errors\":{\"nom\":\"Le nom est obligatoire\"}}");
                return;
            }
            if (parentId != null && findCategory(parentId) == null) {
                json(exchange, 404, "{\"message\":\"Catégorie non trouvée: " + parentId + "\"}");
                return;
            }
            CategoryRow created = new CategoryRow(nextCategoryId.getAndIncrement(), nom, parentId);
            categories.add(created);
            json(exchange, 201, categoryJson(created, false));
            return;
        }
        if ("PUT".equals(method) && path.startsWith("/api/categories/")) {
            Long id = parseId(path);
            String body = readBody(exchange);
            String nom = extractNomFrom(body);
            Long parentId = extractLongField(body, "parentId");
            CategoryRow existing = findCategory(id);
            if (existing == null) {
                json(exchange, 404, "{\"message\":\"Catégorie non trouvée: " + id + "\"}");
                return;
            }
            if (parentId != null && parentId.equals(id)) {
                json(exchange, 400, "{\"message\":\"Une catégorie ne peut pas être son propre parent\"}");
                return;
            }
            if (parentId != null && findCategory(parentId) == null) {
                json(exchange, 404, "{\"message\":\"Catégorie non trouvée: " + parentId + "\"}");
                return;
            }
            existing.nom = nom;
            existing.parentId = parentId;
            json(exchange, 200, categoryJson(existing, false));
            return;
        }
        if ("DELETE".equals(method) && path.startsWith("/api/categories/")) {
            Long id = parseId(path);
            CategoryRow existing = findCategory(id);
            if (existing == null) {
                json(exchange, 404, "{\"message\":\"Catégorie non trouvée: " + id + "\"}");
                return;
            }
            if (!childrenOf(id).isEmpty()) {
                json(exchange, 400, "{\"message\":\"Supprimez d'abord les sous-catégories\"}");
                return;
            }
            if ("Linked".equalsIgnoreCase(existing.nom)) {
                json(exchange, 400, "{\"message\":\"Impossible : des produits sont rattachés à cette catégorie\"}");
                return;
            }
            categories.remove(existing);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        json(exchange, 404, "{\"message\":\"not found\"}");
    }

    private void handleSuppliers(HttpExchange exchange) throws IOException {
        sleep();
        if (!requireAuth(exchange)) {
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();
        if ("GET".equals(method) && path.equals("/api/suppliers/search")) {
            String nom = queryParam(query, "nom").toLowerCase();
            json(exchange, 200, toSupplierArray(suppliers.stream()
                    .filter(s -> s.nom.toLowerCase().contains(nom))
                    .toList()));
            return;
        }
        if ("GET".equals(method) && path.equals("/api/suppliers")) {
            json(exchange, 200, toSupplierArray(suppliers));
            return;
        }
        if ("POST".equals(method) && path.equals("/api/suppliers")) {
            String body = readBody(exchange);
            String nom = extractNomFrom(body);
            if (nom.isBlank()) {
                json(exchange, 400, "{\"errors\":{\"nom\":\"Le nom est obligatoire\"}}");
                return;
            }
            SupplierRow created = new SupplierRow(nextSupplierId.getAndIncrement(), nom);
            applySupplierBody(created, body);
            suppliers.add(created);
            json(exchange, 201, supplierJson(created));
            return;
        }
        if (path.startsWith("/api/suppliers/")) {
            Long id = parseId(path);
            SupplierRow existing = findSupplier(id);
            if (existing == null) {
                json(exchange, 404, "{\"message\":\"Fournisseur non trouvé: " + id + "\"}");
                return;
            }
            if ("PUT".equals(method)) {
                String body = readBody(exchange);
                String nom = extractNomFrom(body);
                if (nom.isBlank()) {
                    json(exchange, 400, "{\"errors\":{\"nom\":\"Le nom est obligatoire\"}}");
                    return;
                }
                existing.nom = nom;
                applySupplierBody(existing, body);
                json(exchange, 200, supplierJson(existing));
                return;
            }
            if ("DELETE".equals(method)) {
                if ("Linked".equalsIgnoreCase(existing.nom)) {
                    json(exchange, 400, "{\"message\":\"Impossible : ce fournisseur est lié à des produits\"}");
                    return;
                }
                suppliers.remove(existing);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
        }
        json(exchange, 404, "{\"message\":\"not found\"}");
    }

    private void handleUnits(HttpExchange exchange) throws IOException {
        sleep();
        if (!requireAuth(exchange)) {
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if ("GET".equals(method) && path.equals("/api/units")) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < units.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                UnitRow u = units.get(i);
                sb.append("{\"id\":").append(u.id).append(",\"nom\":\"").append(u.nom)
                        .append("\",\"symbole\":\"").append(u.symbole).append("\"}");
            }
            json(exchange, 200, sb.append(']').toString());
            return;
        }
        if ("POST".equals(method) && path.equals("/api/units")) {
            String body = readBody(exchange);
            String nom = extractNomFrom(body);
            String symbole = extractStringField(body, "symbole");
            if (nom.isBlank() || symbole == null || symbole.isBlank()) {
                json(exchange, 400, "{\"message\":\"Nom et symbole obligatoires\"}");
                return;
            }
            UnitRow created = new UnitRow(nextUnitId.getAndIncrement(), nom, symbole);
            units.add(created);
            json(exchange, 201, "{\"id\":" + created.id + ",\"nom\":\"" + created.nom
                    + "\",\"symbole\":\"" + created.symbole + "\"}");
            return;
        }
        if ("DELETE".equals(method) && path.startsWith("/api/units/")) {
            Long id = parseId(path);
            UnitRow existing = units.stream().filter(u -> u.id.equals(id)).findFirst().orElse(null);
            if (existing == null) {
                json(exchange, 404, "{\"message\":\"Unité non trouvée: " + id + "\"}");
                return;
            }
            units.remove(existing);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        json(exchange, 404, "{\"message\":\"not found\"}");
    }

    private void handleCustomers(HttpExchange exchange) throws IOException {
        sleep();
        if (!requireAuth(exchange)) {
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();
        if ("GET".equals(method) && path.equals("/api/customers/search")) {
            String q = queryParam(query, "q").toLowerCase();
            json(exchange, 200, toCustomerArray(customers.stream().filter(c ->
                    c.firstName.toLowerCase().contains(q)
                            || c.lastName.toLowerCase().contains(q)
                            || c.email.toLowerCase().contains(q)
            ).toList()));
            return;
        }
        if ("GET".equals(method) && path.equals("/api/customers")) {
            json(exchange, 200, toCustomerArray(customers));
            return;
        }
        if ("POST".equals(method) && path.equals("/api/customers")) {
            String body = readBody(exchange);
            String first = nz(extractStringField(body, "firstName"));
            String last = nz(extractStringField(body, "lastName"));
            if (first.isBlank() || last.isBlank()) {
                json(exchange, 400, "{\"message\":\"Prénom et nom obligatoires\"}");
                return;
            }
            CustomerRow created = new CustomerRow(nextCustomerId.getAndIncrement(), first, last,
                    nz(extractStringField(body, "phone")), nz(extractStringField(body, "email")));
            customers.add(created);
            json(exchange, 201, customerJson(created));
            return;
        }
        if (path.startsWith("/api/customers/")) {
            Long id = parseId(path);
            CustomerRow existing = customers.stream().filter(c -> c.id.equals(id)).findFirst().orElse(null);
            if (existing == null) {
                json(exchange, 404, "{\"message\":\"Client non trouvé: " + id + "\"}");
                return;
            }
            if ("PUT".equals(method)) {
                String body = readBody(exchange);
                existing.firstName = nz(extractStringField(body, "firstName"));
                existing.lastName = nz(extractStringField(body, "lastName"));
                existing.phone = nz(extractStringField(body, "phone"));
                existing.email = nz(extractStringField(body, "email"));
                json(exchange, 200, customerJson(existing));
                return;
            }
            if ("DELETE".equals(method)) {
                customers.remove(existing);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
        }
        json(exchange, 404, "{\"message\":\"not found\"}");
    }

    private void handleStock(HttpExchange exchange) throws IOException {
        sleep();
        if (!requireAuth(exchange)) {
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if ("GET".equals(method) && path.equals("/api/stock/items")) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < stockItems.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                StockItemRow s = stockItems.get(i);
                sb.append("{\"id\":").append(s.id)
                        .append(",\"productId\":").append(s.productId)
                        .append(",\"productNom\":\"").append(s.productNom).append("\"")
                        .append(",\"warehouseCode\":\"").append(s.warehouseCode).append("\"")
                        .append(",\"locationCode\":\"").append(s.locationCode).append("\"")
                        .append(",\"unitSymbole\":\"").append(s.unitSymbole).append("\"")
                        .append(",\"quantityOnHand\":").append(s.quantityOnHand)
                        .append(",\"quantityAvailable\":").append(s.quantityAvailable)
                        .append('}');
            }
            json(exchange, 200, sb.append(']').toString());
            return;
        }
        json(exchange, 404, "{\"message\":\"not found\"}");
    }

    private void handleSettings(HttpExchange exchange) throws IOException {
        sleep();
        if (!requireAuth(exchange)) {
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if ("GET".equals(method) && path.equals("/api/settings")) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < settings.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(settingJson(settings.get(i)));
            }
            json(exchange, 200, sb.append(']').toString());
            return;
        }
        if (path.startsWith("/api/settings/") && path.length() > "/api/settings/".length()) {
            String key = path.substring("/api/settings/".length());
            SettingRow existing = settings.stream().filter(s -> s.key.equals(key)).findFirst().orElse(null);
            if (existing == null) {
                json(exchange, 404, "{\"message\":\"Parametre: " + key + "\"}");
                return;
            }
            if ("GET".equals(method)) {
                json(exchange, 200, settingJson(existing));
                return;
            }
            if ("PUT".equals(method)) {
                String body = readBody(exchange);
                String value = extractStringField(body, "value");
                existing.value = value == null ? "" : value;
                json(exchange, 200, settingJson(existing));
                return;
            }
        }
        json(exchange, 404, "{\"message\":\"not found\"}");
    }

    private void handlePos(HttpExchange exchange) throws IOException {
        sleep();
        if (!requireAuth(exchange)) {
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();
        if ("GET".equals(method) && path.equals("/api/pos/context")) {
            json(exchange, 200, posSessionOpen ? "{\"session\":{\"id\":1}}" : "{\"session\":null}");
            return;
        }
        if ("POST".equals(method) && path.equals("/api/pos/sessions/open")) {
            posSessionOpen = true;
            json(exchange, 201, "{\"id\":1,\"status\":\"OPEN\"}");
            return;
        }
        if ("GET".equals(method) && path.equals("/api/pos/catalog/search")) {
            String q = queryParam(query, "q").toLowerCase();
            StringBuilder sb = new StringBuilder("{\"products\":[");
            boolean first = true;
            for (ProductRow p : products) {
                if (!q.isEmpty() && !p.nom.toLowerCase().contains(q)
                        && (p.sku == null || !p.sku.toLowerCase().contains(q))) {
                    continue;
                }
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append("{\"id\":").append(p.id)
                        .append(",\"nom\":\"").append(p.nom).append("\"");
                appendStr(sb, "sku", p.sku);
                appendNum(sb, "unitPrice", p.prixVente);
                sb.append(",\"stockAvailable\":").append(p.stockTotal)
                        .append(",\"hasVariants\":false}");
            }
            json(exchange, 200, sb.append("]}").toString());
            return;
        }
        if ("POST".equals(method) && path.equals("/api/pos/sales")) {
            SaleRow created = new SaleRow(nextSaleId.getAndIncrement());
            sales.add(created);
            json(exchange, 201, saleJson(created));
            return;
        }
        if (path.startsWith("/api/pos/sales/")) {
            String rest = path.substring("/api/pos/sales/".length());
            String[] segs = rest.split("/");
            Long saleId = Long.parseLong(segs[0]);
            SaleRow sale = sales.stream().filter(s -> s.id.equals(saleId)).findFirst().orElse(null);
            if (sale == null) {
                json(exchange, 404, "{\"message\":\"Vente non trouvée: " + saleId + "\"}");
                return;
            }
            if ("GET".equals(method) && segs.length == 1) {
                json(exchange, 200, saleJson(sale));
                return;
            }
            if ("POST".equals(method) && segs.length == 2 && "lines".equals(segs[1])) {
                String body = readBody(exchange);
                Long productId = extractLongField(body, "productId");
                String qty = extractNumberField(body, "quantityInput");
                ProductRow product = productId == null ? null : findProduct(productId);
                if (product == null) {
                    json(exchange, 404, "{\"message\":\"Produit non trouvé\"}");
                    return;
                }
                SaleLineRow line = new SaleLineRow(nextSaleLineId.getAndIncrement(), product.id, product.nom,
                        qty == null ? "1" : qty, product.prixVente == null ? "0" : product.prixVente);
                sale.lines.add(line);
                json(exchange, 200, saleJson(sale));
                return;
            }
            if ("PUT".equals(method) && segs.length == 3 && "lines".equals(segs[1])) {
                Long lineId = Long.parseLong(segs[2]);
                SaleLineRow line = sale.findLine(lineId);
                if (line == null) {
                    json(exchange, 404, "{\"message\":\"Ligne non trouvée\"}");
                    return;
                }
                String qty = extractNumberField(readBody(exchange), "quantity");
                if (qty != null) {
                    line.quantity = qty;
                }
                json(exchange, 200, saleJson(sale));
                return;
            }
            if ("PUT".equals(method) && segs.length == 4 && "lines".equals(segs[1]) && "discount".equals(segs[3])) {
                Long lineId = Long.parseLong(segs[2]);
                SaleLineRow line = sale.findLine(lineId);
                if (line == null) {
                    json(exchange, 404, "{\"message\":\"Ligne non trouvée\"}");
                    return;
                }
                String amount = extractNumberField(readBody(exchange), "discountAmount");
                line.discount = amount == null ? "0" : amount;
                json(exchange, 200, saleJson(sale));
                return;
            }
            if ("POST".equals(method) && segs.length == 2 && "validate".equals(segs[1])) {
                sale.status = "COMPLETED";
                json(exchange, 200, saleJson(sale));
                return;
            }
            if ("GET".equals(method) && segs.length == 2 && "ticket".equals(segs[1])) {
                json(exchange, 200, "{\"saleNumber\":\"" + sale.saleNumber + "\",\"total\":" + sale.total() + "}");
                return;
            }
        }
        json(exchange, 404, "{\"message\":\"not found\"}");
    }

    private void handleProducts(HttpExchange exchange) throws IOException {
        sleep();
        if (!requireAuth(exchange)) {
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();
        if ("GET".equals(method) && path.equals("/api/products")) {
            json(exchange, 200, toProductArray(filterProducts(query)));
            return;
        }
        if ("POST".equals(method) && path.equals("/api/products")) {
            String body = readBody(exchange);
            String nom = extractNomFrom(body);
            if (nom.isBlank()) {
                json(exchange, 400, "{\"errors\":{\"nom\":\"Le nom est obligatoire\"}}");
                return;
            }
            ProductRow created = new ProductRow(nextProductId.getAndIncrement(), nom);
            applyProductBody(created, body);
            if (created.sku == null || created.sku.isBlank()) {
                created.sku = "SKU-" + created.id;
            }
            if ((created.codeBarre == null || created.codeBarre.isBlank())
                    && Boolean.TRUE.equals(extractBooleanField(body, "generateBarcode"))) {
                created.codeBarre = "1234567890123";
            }
            products.add(created);
            json(exchange, 201, productJson(created));
            return;
        }
        if ("POST".equals(method) && path.equals("/api/products/bulk-delete")) {
            String body = readBody(exchange);
            int deleted = 0;
            for (Long id : parseIdList(body)) {
                ProductRow existing = findProduct(id);
                if (existing != null) {
                    products.remove(existing);
                    deleted++;
                }
            }
            json(exchange, 200, "{\"deletedCount\":" + deleted + "}");
            return;
        }
        String[] segs = path.split("/");
        if (segs.length < 4) {
            json(exchange, 404, "{\"message\":\"not found\"}");
            return;
        }
        Long id = Long.parseLong(segs[3]);
        ProductRow existing = findProduct(id);
        if (existing == null) {
            json(exchange, 404, "{\"message\":\"Produit non trouvé: " + id + "\"}");
            return;
        }
        if ("GET".equals(method) && segs.length == 4) {
            json(exchange, 200, productJson(existing));
            return;
        }
        if ("PUT".equals(method) && segs.length == 4) {
            String body = readBody(exchange);
            String nom = extractNomFrom(body);
            if (nom.isBlank()) {
                json(exchange, 400, "{\"errors\":{\"nom\":\"Le nom est obligatoire\"}}");
                return;
            }
            existing.nom = nom;
            applyProductBody(existing, body);
            json(exchange, 200, productJson(existing));
            return;
        }
        if ("DELETE".equals(method) && segs.length == 4) {
            products.remove(existing);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        if ("GET".equals(method) && segs.length == 5 && "price-history".equals(segs[4])) {
            json(exchange, 200, historyJson(id));
            return;
        }
        if ("PATCH".equals(method) && segs.length == 5 && "price".equals(segs[4])) {
            String body = readBody(exchange);
            String type = extractStringField(body, "type");
            String nouveau = extractNumberField(body, "nouveauPrix");
            if (nouveau == null || nouveau.isBlank()) {
                json(exchange, 400, "{\"message\":\"Le nouveau prix est obligatoire\"}");
                return;
            }
            String ancien = "VENTE".equals(type) ? existing.prixVente
                    : "ACHAT".equals(type) ? existing.prixAchat : existing.prixPromo;
            if ("VENTE".equals(type)) {
                existing.prixVente = nouveau;
            } else if ("ACHAT".equals(type)) {
                existing.prixAchat = nouveau;
            } else {
                existing.prixPromo = nouveau;
            }
            priceHistories.add(new PriceHistoryRow(nextHistoryId.getAndIncrement(), id, type, ancien, nouveau));
            json(exchange, 200, productJson(existing));
            return;
        }
        if ("POST".equals(method) && segs.length == 5 && "images".equals(segs[4])) {
            boolean principale = existing.images.isEmpty();
            ImageRow image = new ImageRow(nextImageId.getAndIncrement(), "photo.png",
                    "/uploads/photo-" + nextImageId.get() + ".png", principale);
            existing.images.add(image);
            json(exchange, 201, image.json());
            return;
        }
        if ("DELETE".equals(method) && segs.length == 6 && "images".equals(segs[4])) {
            Long imageId = Long.parseLong(segs[5]);
            existing.images.removeIf(img -> img.id.equals(imageId));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        json(exchange, 404, "{\"message\":\"not found\"}");
    }

    private boolean requireAuth(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.contains("jwt-test-token")) {
            json(exchange, 401, "{\"message\":\"Authentification requise\"}");
            return false;
        }
        return true;
    }

    private static String permissionsJson() {
        return "[\"dashboard.read\",\"products.read\",\"products.create\",\"products.update\",\"products.delete\","
                + "\"stock.read\",\"customer.read\",\"customer.create\",\"customer.update\",\"customer.delete\","
                + "\"settings.read\",\"settings.update\",\"pos.sale.read\",\"pos.sale.create\",\"pos.session.open\","
                + "\"pos.sale.discount\",\"pos.payment.collect\",\"pos.sale.validate\",\"pos.ticket.print\"]";
    }

    private SupplierRow findSupplier(Long id) {
        return suppliers.stream().filter(s -> s.id.equals(id)).findFirst().orElse(null);
    }

    private void applySupplierBody(SupplierRow row, String body) {
        String email = extractStringField(body, "email");
        if (email != null) {
            row.email = email;
        }
        String tel = extractStringField(body, "telephone");
        if (tel != null) {
            row.telephone = tel;
        }
        String adresse = extractStringField(body, "adresse");
        if (adresse != null) {
            row.adresse = adresse;
        }
    }

    private static String supplierJson(SupplierRow s) {
        return "{\"id\":" + s.id + ",\"nom\":\"" + s.nom + "\",\"email\":\"" + nz(s.email)
                + "\",\"telephone\":\"" + nz(s.telephone) + "\",\"adresse\":\"" + nz(s.adresse) + "\"}";
    }

    private static String toSupplierArray(List<SupplierRow> rows) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(supplierJson(rows.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String customerJson(CustomerRow c) {
        return "{\"id\":" + c.id + ",\"firstName\":\"" + c.firstName + "\",\"lastName\":\"" + c.lastName
                + "\",\"phone\":\"" + nz(c.phone) + "\",\"email\":\"" + nz(c.email)
                + "\",\"companyName\":\"\",\"address\":\"\",\"city\":\"\",\"loyaltyPoints\":0}";
    }

    private static String toCustomerArray(List<CustomerRow> rows) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(customerJson(rows.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String settingJson(SettingRow s) {
        return "{\"key\":\"" + s.key + "\",\"value\":\"" + s.value + "\",\"description\":\""
                + s.description + "\",\"type\":\"" + s.type + "\"}";
    }

    private static String saleJson(SaleRow sale) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":").append(sale.id)
                .append(",\"saleNumber\":\"").append(sale.saleNumber).append("\"")
                .append(",\"status\":\"").append(sale.status).append("\"")
                .append(",\"total\":").append(sale.total())
                .append(",\"discountTotal\":").append(sale.discountTotal())
                .append(",\"hasStockIssues\":false,\"lignes\":[");
        for (int i = 0; i < sale.lines.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            SaleLineRow line = sale.lines.get(i);
            sb.append("{\"id\":").append(line.id)
                    .append(",\"productId\":").append(line.productId)
                    .append(",\"productNom\":\"").append(line.productNom).append("\"")
                    .append(",\"quantityInput\":").append(line.quantity)
                    .append(",\"unitPrice\":").append(line.unitPrice)
                    .append(",\"discountAmount\":").append(line.discount)
                    .append(",\"lineTotal\":").append(line.lineTotal())
                    .append(",\"stockInsufficient\":false}");
        }
        return sb.append("]}").toString();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private BrandRow find(Long id) {
        return brands.stream().filter(b -> b.id.equals(id)).findFirst().orElse(null);
    }

    private static Long parseId(String path) {
        String last = path.substring(path.lastIndexOf('/') + 1);
        return Long.parseLong(last);
    }

    private static String extractNom(HttpExchange exchange) throws IOException {
        return extractNomFrom(readBody(exchange));
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String extractNomFrom(String body) {
        int idx = body.indexOf("\"nom\"");
        if (idx < 0) {
            return "";
        }
        int colon = body.indexOf(':', idx);
        int start = body.indexOf('"', colon + 1);
        int end = body.indexOf('"', start + 1);
        if (start < 0 || end < 0) {
            return "";
        }
        return body.substring(start + 1, end);
    }

    private static Long extractLongField(String body, String field) {
        String key = "\"" + field + "\"";
        int idx = body.indexOf(key);
        if (idx < 0) {
            return null;
        }
        int colon = body.indexOf(':', idx);
        if (colon < 0) {
            return null;
        }
        String rest = body.substring(colon + 1).trim();
        if (rest.startsWith("null")) {
            return null;
        }
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '-' || Character.isDigit(c)) {
                num.append(c);
            } else if (num.length() > 0) {
                break;
            } else if (!Character.isWhitespace(c)) {
                break;
            }
        }
        if (num.length() == 0) {
            return null;
        }
        return Long.parseLong(num.toString());
    }

    private CategoryRow findCategory(Long id) {
        return categories.stream().filter(c -> c.id.equals(id)).findFirst().orElse(null);
    }

    private List<CategoryRow> childrenOf(Long parentId) {
        return categories.stream()
                .filter(c -> parentId != null && parentId.equals(c.parentId))
                .toList();
    }

    private String toCategoryArray(List<CategoryRow> rows, boolean withChildren) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(categoryJson(rows.get(i), withChildren));
        }
        return sb.append(']').toString();
    }

    private String categoryJson(CategoryRow row, boolean withChildren) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":").append(row.id)
                .append(",\"nom\":\"").append(row.nom).append("\"");
        if (row.parentId != null) {
            sb.append(",\"parentId\":").append(row.parentId);
            CategoryRow parent = findCategory(row.parentId);
            if (parent != null) {
                sb.append(",\"parentNom\":\"").append(parent.nom).append("\"");
            }
        }
        if (withChildren) {
            sb.append(",\"children\":[");
            List<CategoryRow> kids = childrenOf(row.id);
            for (int i = 0; i < kids.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(categoryJson(kids.get(i), true));
            }
            sb.append(']');
        }
        return sb.append('}').toString();
    }

    private ProductRow findProduct(Long id) {
        return products.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null);
    }

    private List<ProductRow> filterProducts(String query) {
        String q = queryParam(query, "query").toLowerCase();
        String marque = queryParam(query, "marque").toLowerCase();
        String cycle = queryParam(query, "cycleVie");
        String cat = queryParam(query, "categorieId");
        String sup = queryParam(query, "fournisseurId");
        boolean stockFaible = "true".equalsIgnoreCase(queryParam(query, "stockFaible"));
        boolean rupture = "true".equalsIgnoreCase(queryParam(query, "rupture"));
        return products.stream().filter(p -> {
            if (!q.isEmpty() && !p.nom.toLowerCase().contains(q) && (p.sku == null || !p.sku.toLowerCase().contains(q))) {
                return false;
            }
            if (!marque.isEmpty() && (p.marque == null || !p.marque.toLowerCase().contains(marque))) {
                return false;
            }
            if (!cycle.isEmpty() && (p.cycleVie == null || !p.cycleVie.equals(cycle))) {
                return false;
            }
            if (!cat.isEmpty() && (p.categorieId == null || !cat.equals(String.valueOf(p.categorieId)))) {
                return false;
            }
            if (!sup.isEmpty() && (p.fournisseurId == null || !sup.equals(String.valueOf(p.fournisseurId)))) {
                return false;
            }
            if (stockFaible && p.stockTotal >= 5) {
                return false;
            }
            if (rupture && p.stockTotal > 0) {
                return false;
            }
            return true;
        }).toList();
    }

    private void applyProductBody(ProductRow row, String body) {
        String sku = extractStringField(body, "sku");
        if (sku != null) {
            row.sku = sku;
        }
        String code = extractStringField(body, "codeBarre");
        if (code != null) {
            row.codeBarre = code;
        }
        String desc = extractStringField(body, "description");
        if (desc != null) {
            row.description = desc;
        }
        Long marqueId = extractLongField(body, "marqueId");
        if (marqueId != null) {
            row.marqueId = marqueId;
            BrandRow brand = find(marqueId);
            row.marque = brand == null ? null : brand.nom;
        }
        Long catId = extractLongField(body, "categorieId");
        if (catId != null) {
            row.categorieId = catId;
            CategoryRow cat = findCategory(catId);
            row.categorieNom = cat == null ? null : cat.nom;
        }
        Long supId = extractLongField(body, "fournisseurPrincipalId");
        if (supId != null) {
            row.fournisseurId = supId;
            SupplierRow sup = suppliers.stream().filter(s -> s.id.equals(supId)).findFirst().orElse(null);
            row.fournisseurNom = sup == null ? null : sup.nom;
        }
        Long unitId = extractLongField(body, "unitId");
        if (unitId != null) {
            row.unitId = unitId;
            UnitRow unit = units.stream().filter(u -> u.id.equals(unitId)).findFirst().orElse(null);
            row.unitSymbole = unit == null ? null : unit.symbole;
        }
        String pv = extractNumberField(body, "prixVente");
        if (pv != null) {
            row.prixVente = pv;
        }
        String pa = extractNumberField(body, "prixAchat");
        if (pa != null) {
            row.prixAchat = pa;
        }
        String statut = extractStringField(body, "statut");
        if (statut != null) {
            row.statut = statut;
        }
        String cycle = extractStringField(body, "cycleVie");
        if (cycle != null) {
            row.cycleVie = cycle;
        }
    }

    private String toProductArray(List<ProductRow> rows) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(productJson(rows.get(i)));
        }
        return sb.append(']').toString();
    }

    private String productJson(ProductRow row) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":").append(row.id)
                .append(",\"nom\":\"").append(row.nom).append("\"");
        appendStr(sb, "sku", row.sku);
        appendStr(sb, "codeBarre", row.codeBarre);
        appendStr(sb, "description", row.description);
        if (row.marqueId != null) {
            sb.append(",\"marqueId\":").append(row.marqueId);
        }
        appendStr(sb, "marque", row.marque);
        if (row.categorieId != null) {
            sb.append(",\"categorieId\":").append(row.categorieId);
        }
        appendStr(sb, "categorieNom", row.categorieNom);
        appendNum(sb, "prixAchat", row.prixAchat);
        appendNum(sb, "prixVente", row.prixVente);
        appendNum(sb, "prixPromotionnel", row.prixPromo);
        if (row.fournisseurId != null) {
            sb.append(",\"fournisseurPrincipalId\":").append(row.fournisseurId);
        }
        appendStr(sb, "fournisseurPrincipalNom", row.fournisseurNom);
        if (row.unitId != null) {
            sb.append(",\"unitId\":").append(row.unitId);
        }
        appendStr(sb, "unitSymbole", row.unitSymbole);
        appendStr(sb, "baseUnitSymbole", row.unitSymbole);
        appendStr(sb, "statut", row.statut);
        appendStr(sb, "cycleVie", row.cycleVie);
        sb.append(",\"hasVariants\":false");
        sb.append(",\"stockTotal\":").append(row.stockTotal);
        sb.append(",\"images\":[");
        for (int i = 0; i < row.images.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(row.images.get(i).json());
        }
        sb.append("]}");
        return sb.toString();
    }

    private String historyJson(Long productId) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (PriceHistoryRow row : priceHistories) {
            if (!row.productId.equals(productId)) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"id\":").append(row.id)
                    .append(",\"productId\":").append(row.productId)
                    .append(",\"type\":\"").append(row.type).append("\"");
            appendNum(sb, "ancienPrix", row.ancien);
            appendNum(sb, "nouveauPrix", row.nouveau);
            sb.append(",\"dateModification\":\"2026-08-19T00:00:00Z\"}");
        }
        return sb.append(']').toString();
    }

    private static void appendStr(StringBuilder sb, String field, String value) {
        if (value != null) {
            sb.append(",\"").append(field).append("\":\"").append(value).append("\"");
        }
    }

    private static void appendNum(StringBuilder sb, String field, String value) {
        if (value != null) {
            sb.append(",\"").append(field).append("\":").append(value);
        }
    }

    private static Boolean extractBooleanField(String body, String field) {
        String key = "\"" + field + "\"";
        int idx = body.indexOf(key);
        if (idx < 0) {
            return null;
        }
        int colon = body.indexOf(':', idx);
        String rest = body.substring(colon + 1).trim();
        if (rest.startsWith("true")) {
            return true;
        }
        if (rest.startsWith("false")) {
            return false;
        }
        return null;
    }

    private static String extractStringField(String body, String field) {
        String key = "\"" + field + "\"";
        int idx = body.indexOf(key);
        if (idx < 0) {
            return null;
        }
        int colon = body.indexOf(':', idx);
        int start = body.indexOf('"', colon + 1);
        int end = body.indexOf('"', start + 1);
        if (start < 0 || end < 0) {
            return null;
        }
        return body.substring(start + 1, end);
    }

    private static String extractNumberField(String body, String field) {
        String key = "\"" + field + "\"";
        int idx = body.indexOf(key);
        if (idx < 0) {
            return null;
        }
        int colon = body.indexOf(':', idx);
        String rest = body.substring(colon + 1).trim();
        if (rest.startsWith("null")) {
            return null;
        }
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '-' || c == '.' || Character.isDigit(c)) {
                num.append(c);
            } else if (num.length() > 0) {
                break;
            } else if (!Character.isWhitespace(c)) {
                break;
            }
        }
        return num.length() == 0 ? null : num.toString();
    }

    private static List<Long> parseIdList(String body) {
        List<Long> ids = new ArrayList<>();
        int start = body.indexOf('[');
        int end = body.indexOf(']', start + 1);
        if (start < 0 || end < 0) {
            return ids;
        }
        for (String part : body.substring(start + 1, end).split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                ids.add(Long.parseLong(trimmed));
            }
        }
        return ids;
    }

    private static String queryParam(String query, String name) {
        if (query == null) {
            return "";
        }
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static String toArray(List<BrandRow> rows) {
        StringBuilder sb = new StringBuilder("[");
        Iterator<BrandRow> it = rows.iterator();
        while (it.hasNext()) {
            sb.append(it.next().json());
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        return sb.append(']').toString();
    }

    private void sleep() {
        if (sleepMs > 0) {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void json(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void text(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public static final class BrandRow {
        public final Long id;
        public String nom;

        BrandRow(Long id, String nom) {
            this.id = id;
            this.nom = nom;
        }

        String json() {
            return "{\"id\":" + id + ",\"nom\":\"" + nom + "\"}";
        }
    }

    public static final class CategoryRow {
        public final Long id;
        public String nom;
        public Long parentId;

        CategoryRow(Long id, String nom, Long parentId) {
            this.id = id;
            this.nom = nom;
            this.parentId = parentId;
        }
    }

    public static final class ProductRow {
        public final Long id;
        public String nom;
        public String sku;
        public String codeBarre;
        public String description;
        public Long marqueId;
        public String marque;
        public Long categorieId;
        public String categorieNom;
        public String prixAchat;
        public String prixVente;
        public String prixPromo;
        public Long fournisseurId;
        public String fournisseurNom;
        public Long unitId;
        public String unitSymbole;
        public String statut = "ACTIF";
        public String cycleVie = "BROUILLON";
        public int stockTotal;
        public final List<ImageRow> images = new ArrayList<>();

        ProductRow(Long id, String nom) {
            this.id = id;
            this.nom = nom;
        }
    }

    public static final class ImageRow {
        public final Long id;
        public final String fileName;
        public final String url;
        public final boolean principale;

        ImageRow(Long id, String fileName, String url, boolean principale) {
            this.id = id;
            this.fileName = fileName;
            this.url = url;
            this.principale = principale;
        }

        String json() {
            return "{\"id\":" + id + ",\"fileName\":\"" + fileName + "\",\"url\":\"" + url
                    + "\",\"principale\":" + principale + "}";
        }
    }

    public static final class SupplierRow {
        public final Long id;
        public String nom;
        public String email = "";
        public String telephone = "";
        public String adresse = "";

        SupplierRow(Long id, String nom) {
            this.id = id;
            this.nom = nom;
        }
    }

    public static final class UnitRow {
        public final Long id;
        public String nom;
        public String symbole;

        UnitRow(Long id, String nom, String symbole) {
            this.id = id;
            this.nom = nom;
            this.symbole = symbole;
        }
    }

    public static final class CustomerRow {
        public final Long id;
        public String firstName;
        public String lastName;
        public String phone;
        public String email;

        CustomerRow(Long id, String firstName, String lastName, String phone, String email) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.phone = phone;
            this.email = email;
        }
    }

    public static final class StockItemRow {
        public final Long id;
        public final Long productId;
        public final String productNom;
        public final String warehouseCode;
        public final String locationCode;
        public final String unitSymbole;
        public final String quantityOnHand;
        public final String quantityAvailable;

        StockItemRow(Long id, Long productId, String productNom, String warehouseCode, String locationCode,
                     String unitSymbole, String quantityOnHand, String quantityAvailable) {
            this.id = id;
            this.productId = productId;
            this.productNom = productNom;
            this.warehouseCode = warehouseCode;
            this.locationCode = locationCode;
            this.unitSymbole = unitSymbole;
            this.quantityOnHand = quantityOnHand;
            this.quantityAvailable = quantityAvailable;
        }
    }

    public static final class SettingRow {
        public final String key;
        public String value;
        public final String description;
        public final String type;

        SettingRow(String key, String value, String description, String type) {
            this.key = key;
            this.value = value;
            this.description = description;
            this.type = type;
        }
    }

    public static final class SaleRow {
        public final Long id;
        public final String saleNumber;
        public String status = "DRAFT";
        public final List<SaleLineRow> lines = new ArrayList<>();

        SaleRow(Long id) {
            this.id = id;
            this.saleNumber = "V-" + id;
        }

        SaleLineRow findLine(Long lineId) {
            return lines.stream().filter(l -> l.id.equals(lineId)).findFirst().orElse(null);
        }

        String total() {
            double sum = 0;
            for (SaleLineRow line : lines) {
                sum += Double.parseDouble(line.lineTotal());
            }
            return String.format(java.util.Locale.US, "%.2f", sum);
        }

        String discountTotal() {
            double sum = 0;
            for (SaleLineRow line : lines) {
                sum += Double.parseDouble(line.discount);
            }
            return String.format(java.util.Locale.US, "%.2f", sum);
        }
    }

    public static final class SaleLineRow {
        public final Long id;
        public final Long productId;
        public final String productNom;
        public String quantity;
        public final String unitPrice;
        public String discount = "0";

        SaleLineRow(Long id, Long productId, String productNom, String quantity, String unitPrice) {
            this.id = id;
            this.productId = productId;
            this.productNom = productNom;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        String lineTotal() {
            double qty = Double.parseDouble(quantity);
            double price = Double.parseDouble(unitPrice);
            double disc = Double.parseDouble(discount);
            return String.format(java.util.Locale.US, "%.2f", qty * price - disc);
        }
    }

    public static final class PriceHistoryRow {
        public final Long id;
        public final Long productId;
        public final String type;
        public final String ancien;
        public final String nouveau;

        PriceHistoryRow(Long id, Long productId, String type, String ancien, String nouveau) {
            this.id = id;
            this.productId = productId;
            this.type = type;
            this.ancien = ancien;
            this.nouveau = nouveau;
        }
    }
}
