package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.PriceHistory;
import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.ProductDraft;
import com.gestpov.desktop.model.ProductImage;
import com.gestpov.desktop.model.ProductPackaging;
import com.gestpov.desktop.model.ProductQuery;
import com.gestpov.desktop.model.ProductVariant;
import com.gestpov.desktop.model.AuditLogEntry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductClient {

    private final ApiClient api;

    public ProductClient(ApiClient api) {
        this.api = api;
    }

    public List<Product> search(ProductQuery query) throws ApiException {
        Map<String, String> params = query == null ? Map.of() : query.toParams();
        JsonNode node = params.isEmpty() ? api.get("/api/products") : api.get("/api/products", params);
        return parseProducts(node);
    }

    public Product getById(long id) throws ApiException {
        return Product.fromJson(api.get("/api/products/" + id));
    }

    public Product create(ProductDraft draft) throws ApiException {
        return Product.fromJson(api.post("/api/products", draft.toBody(true)));
    }

    public Product update(long id, ProductDraft draft) throws ApiException {
        return Product.fromJson(api.put("/api/products/" + id, draft.toBody(false)));
    }

    public void delete(long id) throws ApiException {
        api.delete("/api/products/" + id);
    }

    public int bulkDelete(List<Long> ids) throws ApiException {
        JsonNode node = api.post("/api/products/bulk-delete", Map.of("ids", ids));
        return node.path("deletedCount").asInt(ids == null ? 0 : ids.size());
    }

    public Product updatePrice(long id, String type, BigDecimal nouveauPrix) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("nouveauPrix", nouveauPrix);
        return Product.fromJson(api.patch("/api/products/" + id + "/price", body));
    }

    public List<PriceHistory> priceHistory(long id) throws ApiException {
        JsonNode node = api.get("/api/products/" + id + "/price-history");
        List<PriceHistory> rows = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                PriceHistory row = PriceHistory.fromJson(item);
                if (row != null) {
                    rows.add(row);
                }
            });
        }
        return rows;
    }

    public List<AuditLogEntry> auditHistory(long id) throws ApiException {
        return JsonLists.mapArray(api.get("/api/products/" + id + "/audit"), AuditLogEntry::fromJson);
    }

    public Product submitLifecycle(long id) throws ApiException {
        return Product.fromJson(api.post("/api/products/" + id + "/lifecycle/submit", Map.of()));
    }

    public Product approveLifecycle(long id) throws ApiException {
        return Product.fromJson(api.post("/api/products/" + id + "/lifecycle/approve", Map.of()));
    }

    public Product rejectLifecycle(long id, String reason) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        if (reason != null && !reason.isBlank()) {
            body.put("reason", reason);
        }
        return Product.fromJson(api.post("/api/products/" + id + "/lifecycle/reject", body));
    }

    public List<ProductVariant> listVariants(long productId) throws ApiException {
        Product product = getById(productId);
        return product == null || product.variantes() == null ? List.of() : product.variantes();
    }

    public ProductVariant addVariant(long productId, String couleur, String taille, String sku,
                                     BigDecimal prix, boolean generateBarcode) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("couleur", emptyToNull(couleur));
        body.put("taille", emptyToNull(taille));
        body.put("sku", emptyToNull(sku));
        if (prix != null) {
            body.put("prix", prix);
        }
        body.put("generateBarcode", generateBarcode);
        body.put("active", true);
        return ProductVariant.fromJson(api.post("/api/products/" + productId + "/variants", body));
    }

    public void deleteVariant(long productId, long variantId) throws ApiException {
        api.delete("/api/products/" + productId + "/variants/" + variantId);
    }

    public List<ProductPackaging> listPackagings(long productId) throws ApiException {
        return JsonLists.mapArray(api.get("/api/products/" + productId + "/packagings"), ProductPackaging::fromJson);
    }

    public ProductPackaging addPackaging(long productId, String nom, String symbole, BigDecimal quantiteBase,
                                         BigDecimal prixVente) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nom", nom);
        body.put("symbole", emptyToNull(symbole));
        body.put("quantiteBase", quantiteBase);
        if (prixVente != null) {
            body.put("prixVente", prixVente);
        }
        body.put("usableForSale", true);
        body.put("usableForPurchase", true);
        body.put("actif", true);
        return ProductPackaging.fromJson(api.post("/api/products/" + productId + "/packagings", body));
    }

    public void deletePackaging(long productId, long packagingId) throws ApiException {
        api.delete("/api/products/" + productId + "/packagings/" + packagingId);
    }

    public ProductImage uploadImage(long productId, String fileName, byte[] bytes, boolean principale)
            throws ApiException {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("principale", Boolean.toString(principale));
        return ProductImage.fromJson(api.postMultipart(
                "/api/products/" + productId + "/images", "file", fileName, bytes, fields));
    }

    /**
     * Marque une image existante comme principale en la ré-uploadant (seul mécanisme API).
     */
    public ProductImage setPrimaryImage(long productId, String fileName, byte[] bytes) throws ApiException {
        return uploadImage(productId, fileName, bytes, true);
    }

    public void deleteImage(long productId, long imageId) throws ApiException {
        api.delete("/api/products/" + productId + "/images/" + imageId);
    }

    static List<Product> parseProducts(JsonNode node) {
        List<Product> products = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return products;
        }
        node.forEach(item -> {
            Product product = Product.fromJson(item);
            if (product != null) {
                products.add(product);
            }
        });
        return products;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
