package com.erp.products.controller;

import com.erp.products.domain.enums.DocumentType;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import com.erp.products.dto.*;
import com.erp.products.service.PackagingService;
import com.erp.products.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final PackagingService packagingService;

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping
    public List<ProductResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String codeBarre,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false) String marque,
            @RequestParam(required = false) Long fournisseurId,
            @RequestParam(required = false) ProductStatus statut,
            @RequestParam(required = false) LifecycleStatus cycleVie,
            @RequestParam(required = false) Boolean stockFaible,
            @RequestParam(required = false) Boolean rupture,
            @RequestParam(required = false) BigDecimal prixMin,
            @RequestParam(required = false) BigDecimal prixMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedTo,
            @RequestParam(required = false) Integer stockSeuil) {

        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setQuery(query);
        criteria.setSku(sku);
        criteria.setCodeBarre(codeBarre);
        criteria.setCategorieId(categorieId);
        criteria.setMarque(marque);
        criteria.setFournisseurId(fournisseurId);
        criteria.setStatut(statut);
        criteria.setCycleVie(cycleVie);
        criteria.setStockFaible(stockFaible);
        criteria.setRupture(rupture);
        criteria.setPrixMin(prixMin);
        criteria.setPrixMax(prixMax);
        criteria.setCreatedFrom(createdFrom);
        criteria.setCreatedTo(createdTo);
        criteria.setUpdatedFrom(updatedFrom);
        criteria.setUpdatedTo(updatedTo);
        criteria.setStockSeuil(stockSeuil);

        return productService.search(criteria);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/sku/{sku}")
    public ProductResponse getBySku(@PathVariable String sku) {
        return productService.getBySku(sku);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/barcode/{codeBarre}")
    public ProductResponse getByBarcode(@PathVariable String codeBarre) {
        return productService.getByBarcode(codeBarre);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    @PatchMapping("/{id}/category")
    public ProductResponse moveCategory(@PathVariable Long id, @Valid @RequestBody MoveCategoryRequest request) {
        return productService.moveCategory(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    @PatchMapping("/{id}/lifecycle")
    public ProductResponse updateLifecycle(@PathVariable Long id, @Valid @RequestBody LifecycleUpdateRequest request) {
        return productService.updateLifecycle(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    @PatchMapping("/{id}/price")
    public ProductResponse updatePrice(@PathVariable Long id, @Valid @RequestBody PriceUpdateRequest request) {
        return productService.updatePrice(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/{id}/price-history")
    public List<PriceHistoryResponse> getPriceHistory(@PathVariable Long id) {
        return productService.getPriceHistory(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/{id}/audit")
    public List<AuditLogResponse> getAuditHistory(@PathVariable Long id) {
        return productService.getAuditHistory(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping("/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductVariantResponse addVariant(@PathVariable Long id, @Valid @RequestBody ProductVariantRequest request) {
        return productService.addVariant(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    @PutMapping("/{id}/variants/{variantId}")
    public ProductVariantResponse updateVariant(@PathVariable Long id, @PathVariable Long variantId,
                                                 @Valid @RequestBody ProductVariantRequest request) {
        return productService.updateVariant(id, variantId, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/{id}/variants/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(@PathVariable Long id, @PathVariable Long variantId) {
        productService.deleteVariant(id, variantId);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping("/{id}/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductSupplierResponse addSupplier(@PathVariable Long id, @Valid @RequestBody ProductSupplierRequest request) {
        return productService.addSupplier(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/{id}/suppliers/{productSupplierId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSupplier(@PathVariable Long id, @PathVariable Long productSupplierId) {
        productService.removeSupplier(id, productSupplierId);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductImageResponse addImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean principale,
            @RequestParam(required = false) Integer ordre) {
        return productService.addImage(id, file, principale, ordre);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        productService.deleteImage(id, imageId);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDocumentResponse addDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam DocumentType type) {
        return productService.addDocument(id, file, type);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/{id}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable Long id, @PathVariable Long documentId) {
        productService.deleteDocument(id, documentId);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/{id}/packagings")
    public List<ProductPackagingResponse> listPackagings(@PathVariable Long id) {
        return packagingService.listByProduct(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping("/{id}/packagings")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductPackagingResponse addPackaging(@PathVariable Long id,
                                                  @Valid @RequestBody ProductPackagingRequest request) {
        return packagingService.create(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    @PutMapping("/{id}/packagings/{packagingId}")
    public ProductPackagingResponse updatePackaging(@PathVariable Long id,
                                                     @PathVariable Long packagingId,
                                                     @Valid @RequestBody ProductPackagingRequest request) {
        return packagingService.update(id, packagingId, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/{id}/packagings/{packagingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePackaging(@PathVariable Long id, @PathVariable Long packagingId) {
        packagingService.delete(id, packagingId);
    }

    /** Simule une réception en conditionnement → stock en unité de base */
    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @PostMapping("/{id}/packagings/convert-to-base")
    public PackagingToBaseResponse convertPackagingToBase(@PathVariable Long id,
                                                           @Valid @RequestBody PackagingToBaseRequest request) {
        return packagingService.convertToBase(id, request);
    }
}
