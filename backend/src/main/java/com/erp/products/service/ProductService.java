package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.*;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.ProductMapper;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final ProductImageRepository imageRepository;
    private final ProductDocumentRepository documentRepository;
    private final ProductCustomAttributeValueRepository attributeValueRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final UnitOfMeasureRepository unitRepository;
    private final CustomAttributeDefinitionRepository attributeDefinitionRepository;
    private final CategoryService categoryService;
    private final SupplierService supplierService;
    private final BrandService brandService;
    private final ProductMapper mapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final BarcodeService barcodeService;
    private final FileStorageService fileStorageService;
    private final ProductVariantAttributeService variantAttributeService;
    private final ProductVariantPolicyService variantPolicyService;
    private final BarcodeRegistryService barcodeRegistryService;

    @Transactional(readOnly = true)
    public List<ProductResponse> search(ProductSearchCriteria criteria) {
        return productRepository.findAll(ProductSpecification.fromCriteria(criteria)).stream()
                .map(p -> mapper.toProductResponse(p, null, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return mapper.toProductResponse(findProduct(id), barcodeService, true);
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + sku));
        return mapper.toProductResponse(product, barcodeService, true);
    }

    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String codeBarre) {
        String normalized = BarcodeRegistryService.normalize(codeBarre);
        if (normalized == null) {
            throw new ResourceNotFoundException("Produit non trouvé pour le code-barres: " + codeBarre);
        }
        Optional<Product> byProduct = productRepository.findActiveSimpleByCodeBarre(normalized);
        if (byProduct.isPresent()) {
            return mapper.toProductResponse(byProduct.get(), barcodeService, true);
        }
        Product product = productRepository.findByVariantBarcode(normalized)
                .or(() -> variantRepository.findByCodeBarreNormalized(normalized).map(ProductVariant::getProduct))
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé pour le code-barres: " + codeBarre));
        return mapper.toProductResponse(product, barcodeService, true);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = resolveProductSku(request.getSku(), request.getNom());

        Product product = Product.builder()
                .nom(request.getNom())
                .sku(sku)
                .description(request.getDescription())
                .prixAchat(request.getPrixAchat())
                .prixVente(request.getPrixVente())
                .prixPromotionnel(request.getPrixPromotionnel())
                .prixPromotionnelDebut(request.getPrixPromotionnelDebut())
                .prixPromotionnelFin(request.getPrixPromotionnelFin())
                .statut(request.getStatut() != null ? request.getStatut() : ProductStatus.ACTIF)
                .cycleVie(request.getCycleVie() != null ? request.getCycleVie() : LifecycleStatus.BROUILLON)
                .build();

        applyRelations(product, request);
        Product saved = productRepository.save(product);

        if (request.getVariantes() != null) {
            Set<String> reservedVariantSkus = new HashSet<>();
            for (int i = 0; i < request.getVariantes().size(); i++) {
                addVariantInternal(saved, request.getVariantes().get(i),
                        currentUserService.resolveActor(request.getUtilisateur()),
                        reservedVariantSkus, i + 1);
            }
        }

        variantAttributeService.syncProductSellable(saved.getId());
        Product productForExtras = findProduct(saved.getId());
        applyProductBarcode(productForExtras, request);
        productRepository.save(productForExtras);

        if (request.getFournisseurs() != null) {
            request.getFournisseurs().forEach(f -> addSupplierInternal(productForExtras, f));
        }

        if (request.getAttributs() != null) {
            saveAttributes(productForExtras, request.getAttributs());
        }

        auditService.log("Product", productForExtras.getId(), AuditAction.CREATION,
                "Produit créé: " + productForExtras.getNom(), request.getUtilisateur());

        return mapper.toProductResponse(findProduct(productForExtras.getId()), barcodeService, true);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);

        if (isNotBlank(request.getSku()) && !product.getSku().equals(request.getSku().trim())
                && productRepository.existsBySku(request.getSku().trim())) {
            throw new BusinessException("SKU déjà existant: " + request.getSku());
        }

        product.setNom(request.getNom());
        if (isNotBlank(request.getSku())) {
            product.setSku(request.getSku().trim());
        }
        product.setDescription(request.getDescription());
        product.setPrixAchat(request.getPrixAchat());
        product.setPrixVente(request.getPrixVente());
        product.setPrixPromotionnel(request.getPrixPromotionnel());
        product.setPrixPromotionnelDebut(request.getPrixPromotionnelDebut());
        product.setPrixPromotionnelFin(request.getPrixPromotionnelFin());

        if (request.getStatut() != null) {
            product.setStatut(request.getStatut());
        }
        if (request.getCycleVie() != null) {
            product.setCycleVie(request.getCycleVie());
        }

        applyRelations(product, request);
        applyProductBarcode(product, request);

        if (request.getAttributs() != null) {
            product.getAttributs().clear();
            saveAttributes(product, request.getAttributs());
        }

        Product saved = productRepository.save(product);
        auditService.log("Product", saved.getId(), AuditAction.MODIFICATION,
                "Produit modifié: " + saved.getNom(), request.getUtilisateur());

        return mapper.toProductResponse(findProduct(saved.getId()), barcodeService, true);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        priceHistoryRepository.deleteByProductId(id);
        auditLogRepository.deleteByEntityTypeAndEntityId("Product", id);
        product.getImages().forEach(img -> fileStorageService.delete(img.getFilePath()));
        product.getDocuments().forEach(doc -> fileStorageService.delete(doc.getFilePath()));
        productRepository.delete(product);
        auditService.log("Product", id, AuditAction.SUPPRESSION, "Produit supprimé: " + product.getNom());
    }

    @Transactional
    public ProductBulkDeleteResponse deleteMany(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("Aucun produit à supprimer");
        }
        List<Long> distinctIds = ids.stream().filter(id -> id != null).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException("Aucun produit à supprimer");
        }
        for (Long id : distinctIds) {
            delete(id);
        }
        return ProductBulkDeleteResponse.builder().deletedCount(distinctIds.size()).build();
    }

    @Transactional
    public ProductResponse moveCategory(Long id, MoveCategoryRequest request) {
        Product product = findProduct(id);
        Category oldCategory = product.getCategorie();
        Category newCategory = categoryService.findCategory(request.getCategorieId());
        product.setCategorie(newCategory);
        Product saved = productRepository.save(product);

        String details = String.format("Catégorie changée de '%s' vers '%s'",
                oldCategory != null ? oldCategory.getNom() : "aucune",
                newCategory.getNom());
        auditService.log("Product", saved.getId(), AuditAction.CHANGEMENT_CATEGORIE, details, request.getUtilisateur());

        return mapper.toProductResponse(findProduct(saved.getId()), barcodeService, true);
    }

    @Transactional
    public ProductResponse updateLifecycle(Long id, LifecycleUpdateRequest request) {
        Product product = findProduct(id);
        LifecycleStatus old = product.getCycleVie();
        LifecycleStatus target = request.getCycleVie();

        if (target == LifecycleStatus.ACTIF) {
            throw new BusinessException("Utilisez l'action « Valider » pour activer le produit au catalogue");
        }
        if (target == LifecycleStatus.EN_ATTENTE_VALIDATION && old != LifecycleStatus.BROUILLON) {
            throw new BusinessException("Seul un brouillon peut être soumis à validation");
        }

        product.setCycleVie(target);
        Product saved = productRepository.save(product);

        auditService.log("Product", saved.getId(), AuditAction.CHANGEMENT_CYCLE_VIE,
                "Cycle de vie: " + old + " -> " + target, request.getUtilisateur());

        return mapper.toProductResponse(findProduct(saved.getId()), barcodeService, true);
    }

    @Transactional
    public ProductResponse submitForValidation(Long id, String utilisateur) {
        Product product = findProduct(id);
        if (product.getCycleVie() != LifecycleStatus.BROUILLON) {
            throw new BusinessException("Seul un produit brouillon peut être soumis");
        }
        assertProductCompleteness(product);
        product.setCycleVie(LifecycleStatus.EN_ATTENTE_VALIDATION);
        Product saved = productRepository.save(product);
        auditService.log("Product", saved.getId(), AuditAction.CHANGEMENT_CYCLE_VIE,
                "Soumis à validation", utilisateur);
        return mapper.toProductResponse(findProduct(saved.getId()), barcodeService, true);
    }

    @Transactional
    public ProductResponse approveLifecycle(Long id, String utilisateur) {
        Product product = findProduct(id);
        if (product.getCycleVie() != LifecycleStatus.EN_ATTENTE_VALIDATION) {
            throw new BusinessException("Seul un produit en attente de validation peut être approuvé");
        }
        assertProductCompleteness(product);
        product.setCycleVie(LifecycleStatus.ACTIF);
        product.setStatut(ProductStatus.ACTIF);
        Product saved = productRepository.save(product);
        auditService.log("Product", saved.getId(), AuditAction.CHANGEMENT_CYCLE_VIE,
                "Produit validé et activé", utilisateur);
        return mapper.toProductResponse(findProduct(saved.getId()), barcodeService, true);
    }

    @Transactional
    public ProductResponse rejectLifecycle(Long id, LifecycleRejectRequest request) {
        Product product = findProduct(id);
        if (product.getCycleVie() != LifecycleStatus.EN_ATTENTE_VALIDATION) {
            throw new BusinessException("Seul un produit en attente peut être rejeté");
        }
        product.setCycleVie(LifecycleStatus.BROUILLON);
        Product saved = productRepository.save(product);
        String details = request.getReason() != null && !request.getReason().isBlank()
                ? "Rejeté: " + request.getReason().trim()
                : "Rejeté — retour brouillon";
        auditService.log("Product", saved.getId(), AuditAction.CHANGEMENT_CYCLE_VIE,
                details, request.getUtilisateur());
        return mapper.toProductResponse(findProduct(saved.getId()), barcodeService, true);
    }

    private void assertProductCompleteness(Product product) {
        if (product.getNom() == null || product.getNom().isBlank()) {
            throw new BusinessException("Nom produit obligatoire");
        }
        if (product.getSku() == null || product.getSku().isBlank()) {
            throw new BusinessException("SKU obligatoire");
        }
        if (product.getUnit() == null) {
            throw new BusinessException("Unité de base obligatoire");
        }
        if (product.getPrixVente() == null) {
            throw new BusinessException("Prix de vente obligatoire");
        }
    }

    @Transactional
    public ProductResponse updatePrice(Long id, PriceUpdateRequest request) {
        Product product = findProduct(id);
        BigDecimal oldPrice = getCurrentPrice(product, request.getType());
        setPrice(product, request.getType(), request.getNouveauPrix());
        Product saved = productRepository.save(product);

        savePriceHistory(saved, null, request.getType(), oldPrice, request.getNouveauPrix(),
                currentUserService.resolveActor(request.getUtilisateur()));
        auditService.log("Product", saved.getId(), AuditAction.CHANGEMENT_PRIX,
                request.getType() + ": " + oldPrice + " -> " + request.getNouveauPrix(), request.getUtilisateur());

        return mapper.toProductResponse(findProduct(saved.getId()), barcodeService, true);
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(Long productId) {
        return priceHistoryRepository.findByProductIdOrderByDateModificationDesc(productId).stream()
                .map(mapper::toPriceHistoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductVariantResponse addVariant(Long productId, ProductVariantRequest request) {
        Product product = findProduct(productId);
        ProductVariant variant = addVariantInternal(product, request, "system", new HashSet<>(),
                variantRepository.findByProductId(productId).size() + 1);
        return mapper.toVariantResponse(variant, barcodeService);
    }

    @Transactional
    public ProductVariantResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request) {
        ProductVariant variant = findVariant(productId, variantId);

        if (isNotBlank(request.getSku()) && !variant.getSku().equals(request.getSku().trim())
                && variantRepository.existsBySku(request.getSku().trim())) {
            throw new BusinessException("SKU variante déjà existant: " + request.getSku());
        }

        variant.setCouleur(request.getCouleur());
        variant.setTaille(request.getTaille());
        if (isNotBlank(request.getSku())) {
            variant.setSku(request.getSku().trim());
        }
        if (request.getPrix() != null) {
            variant.setPrix(request.getPrix());
        }
        if (request.getStock() != null) {
            variant.setStock(request.getStock());
        }
        applyVariantFlags(variant, request);
        variantAttributeService.applySelections(variant, variantAttributeService.resolveSelections(request));
        variant.setName(variantAttributeService.buildVariantLabel(variant));
        applyBarcode(variant, request);
        variantAttributeService.assertVariantUniqueness(variant);

        return mapper.toVariantResponse(variantRepository.save(variant), barcodeService);
    }

    @Transactional
    public List<ProductVariantResponse> generateVariants(Long productId, ProductVariantGenerateRequest request) {
        Product product = findProduct(productId);
        List<ProductVariant> drafts = variantAttributeService.generateVariants(product, request);
        Set<String> reservedVariantSkus = variantRepository.findByProductId(productId).stream()
                .map(ProductVariant::getSku)
                .collect(Collectors.toCollection(HashSet::new));

        List<ProductVariantResponse> created = new java.util.ArrayList<>();
        for (ProductVariant draft : drafts) {
            String sku = ProductSkuGenerator.ensureUnique(draft.getSku(), candidate ->
                    reservedVariantSkus.contains(candidate) || variantRepository.existsBySku(candidate));
            draft.setSku(sku);
            reservedVariantSkus.add(sku);
            ProductVariant saved = variantRepository.save(draft);
            product.getVariantes().add(saved);
            created.add(mapper.toVariantResponse(saved, barcodeService));
        }
        variantAttributeService.syncProductSellable(productId);
        auditService.log("Product", productId, AuditAction.AJOUT_VARIANTE,
                "Variantes générées: " + created.size());
        return created;
    }

    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant variant = findVariant(productId, variantId);
        variantPolicyService.assertDeletable(variant);
        priceHistoryRepository.deleteByVariantId(variantId);
        variantRepository.delete(variant);
        variantAttributeService.syncProductSellable(productId);
        auditService.log("Product", productId, AuditAction.SUPPRESSION_VARIANTE,
                "Variante supprimée: " + variant.getSku());
    }

    @Transactional
    public ProductSupplierResponse addSupplier(Long productId, ProductSupplierRequest request) {
        Product product = findProduct(productId);
        ProductSupplier ps = addSupplierInternal(product, request);
        return mapper.toProductSupplierResponse(ps);
    }

    @Transactional
    public void removeSupplier(Long productId, Long productSupplierId) {
        ProductSupplier ps = productSupplierRepository.findById(productSupplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Liaison fournisseur non trouvée"));
        if (!ps.getProduct().getId().equals(productId)) {
            throw new BusinessException("Liaison invalide pour ce produit");
        }
        productSupplierRepository.delete(ps);
        auditService.log("Product", productId, AuditAction.SUPPRESSION_FOURNISSEUR,
                "Fournisseur retiré: " + ps.getSupplier().getNom());
    }

    @Transactional
    public ProductImageResponse addImage(Long productId, MultipartFile file, boolean principale, Integer ordre) {
        Product product = findProduct(productId);
        String path = fileStorageService.store(file, "images/products/" + productId);

        if (principale) {
            product.getImages().forEach(img -> img.setPrincipale(false));
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .fileName(file.getOriginalFilename())
                .filePath(path)
                .principale(principale)
                .ordre(ordre)
                .build();

        ProductImage saved = imageRepository.save(image);
        auditService.log("Product", productId, AuditAction.AJOUT_IMAGE, "Image ajoutée: " + saved.getFileName());
        return mapper.toImageResponse(saved);
    }

    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image non trouvée"));
        if (!image.getProduct().getId().equals(productId)) {
            throw new BusinessException("Image invalide pour ce produit");
        }
        fileStorageService.delete(image.getFilePath());
        imageRepository.delete(image);
        auditService.log("Product", productId, AuditAction.SUPPRESSION_IMAGE, "Image supprimée");
    }

    @Transactional
    public ProductDocumentResponse addDocument(Long productId, MultipartFile file, DocumentType type) {
        Product product = findProduct(productId);
        String path = fileStorageService.store(file, "documents/products/" + productId);

        ProductDocument doc = ProductDocument.builder()
                .product(product)
                .fileName(file.getOriginalFilename())
                .filePath(path)
                .type(type)
                .build();

        ProductDocument saved = documentRepository.save(doc);
        auditService.log("Product", productId, AuditAction.AJOUT_DOCUMENT, "Document ajouté: " + saved.getFileName());
        return mapper.toDocumentResponse(saved);
    }

    @Transactional
    public void deleteDocument(Long productId, Long documentId) {
        ProductDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé"));
        if (!doc.getProduct().getId().equals(productId)) {
            throw new BusinessException("Document invalide pour ce produit");
        }
        fileStorageService.delete(doc.getFilePath());
        documentRepository.delete(doc);
        auditService.log("Product", productId, AuditAction.SUPPRESSION_DOCUMENT, "Document supprimé");
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditHistory(Long productId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByDateActionDesc("Product", productId).stream()
                .map(mapper::toAuditResponse)
                .collect(Collectors.toList());
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + id));
    }

    private ProductVariant findVariant(Long productId, Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvée"));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException("Variante invalide pour ce produit");
        }
        return variant;
    }

    private void applyRelations(Product product, ProductRequest request) {
        if (request.getCategorieId() != null) {
            product.setCategorie(categoryService.findCategory(request.getCategorieId()));
        } else {
            product.setCategorie(null);
        }

        if (request.getMarqueId() != null) {
            product.setBrand(brandService.findBrand(request.getMarqueId()));
        } else {
            product.setBrand(null);
        }

        if (request.getFournisseurPrincipalId() != null) {
            product.setFournisseurPrincipal(supplierService.findSupplier(request.getFournisseurPrincipalId()));
        } else {
            product.setFournisseurPrincipal(null);
        }

        if (request.getUnitId() != null) {
            product.setUnit(unitRepository.findById(request.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unité non trouvée")));
        } else {
            product.setUnit(null);
        }
    }

    private ProductVariant addVariantInternal(
            Product product,
            ProductVariantRequest request,
            String utilisateur,
            Set<String> reservedVariantSkus,
            int variantIndex) {
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .couleur(request.getCouleur())
                .taille(request.getTaille())
                .prix(request.getPrix() != null ? request.getPrix() : product.getPrixVente())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .isSellable(true)
                .isStockable(true)
                .isActive(true)
                .build();

        applyVariantFlags(variant, request);
        variantAttributeService.applySelections(variant, variantAttributeService.resolveSelections(request));
        variant.setName(variantAttributeService.buildVariantLabel(variant));

        String sku;
        if (isNotBlank(request.getSku())) {
            sku = request.getSku().trim();
            if (reservedVariantSkus.contains(sku) || variantRepository.existsBySku(sku)) {
                throw new BusinessException("SKU variante déjà existant: " + sku);
            }
        } else {
            sku = variantAttributeService.buildVariantSku(product, variant, variantIndex);
            sku = ProductSkuGenerator.ensureUnique(sku, candidate ->
                    reservedVariantSkus.contains(candidate) || variantRepository.existsBySku(candidate));
        }
        variant.setSku(sku);
        reservedVariantSkus.add(sku);

        applyBarcode(variant, request);
        variantAttributeService.assertVariantUniqueness(variant);
        ProductVariant saved = variantRepository.save(variant);
        product.getVariantes().add(saved);
        variantAttributeService.syncProductSellable(product.getId());
        auditService.log("Product", product.getId(), AuditAction.AJOUT_VARIANTE,
                "Variante ajoutée: " + saved.getSku(), currentUserService.resolveActor(utilisateur));
        return saved;
    }

    private void applyVariantFlags(ProductVariant variant, ProductVariantRequest request) {
        if (request.getCostPrice() != null) {
            variant.setCostPrice(request.getCostPrice());
        }
        if (request.getSellable() != null) {
            variant.setIsSellable(request.getSellable());
        }
        if (request.getStockable() != null) {
            variant.setIsStockable(request.getStockable());
        }
        if (request.getActive() != null) {
            variant.setIsActive(request.getActive());
        }
    }

    private void applyProductBarcode(Product product, ProductRequest request) {
        if (variantPolicyService.hasVariants(product)) {
            if (product.getCodeBarre() != null) {
                product.setCodeBarre(null);
            }
            return;
        }
        if (request.getCodeBarre() != null && !request.getCodeBarre().isBlank()) {
            String code = request.getCodeBarre().trim();
            barcodeRegistryService.assertAvailable(code, product.getId(), null, null);
            product.setCodeBarre(code);
            return;
        }
        if (request.getCodeBarre() != null && request.getCodeBarre().isBlank()) {
            product.setCodeBarre(null);
            return;
        }
        if (Boolean.FALSE.equals(request.getGenerateBarcode())) {
            return;
        }
        if (Boolean.TRUE.equals(request.getGenerateBarcode()) || product.getCodeBarre() == null) {
            product.setCodeBarre(barcodeService.allocateEan13(barcodeRegistryService::isTaken));
        }
    }

    private void applyBarcode(ProductVariant variant, ProductVariantRequest request) {
        if (request.getCodeBarre() != null && !request.getCodeBarre().isBlank()) {
            variant.setCodeBarre(request.getCodeBarre().trim());
            variant.setBarcodeType(request.getBarcodeType() != null ? request.getBarcodeType() : BarcodeType.EAN13);
            return;
        }
        if (request.getCodeBarre() != null && request.getCodeBarre().isBlank()) {
            variant.setCodeBarre(null);
            variant.setBarcodeType(null);
            return;
        }
        if (Boolean.FALSE.equals(request.getGenerateBarcode())) {
            return;
        }
        BarcodeType type = request.getBarcodeType() != null ? request.getBarcodeType() : BarcodeType.EAN13;
        variant.setBarcodeType(type);
        if (type == BarcodeType.EAN13) {
            variant.setCodeBarre(barcodeService.allocateEan13(barcodeRegistryService::isTaken));
        } else {
            variant.setCodeBarre(variant.getSku());
        }
    }

    private ProductSupplier addSupplierInternal(Product product, ProductSupplierRequest request) {
        Supplier supplier = supplierService.findSupplier(request.getSupplierId());

        if (Boolean.TRUE.equals(request.getPrincipal())) {
            product.getFournisseurs().forEach(ps -> ps.setPrincipal(false));
            product.setFournisseurPrincipal(supplier);
        }

        ProductSupplier ps = ProductSupplier.builder()
                .product(product)
                .supplier(supplier)
                .principal(Boolean.TRUE.equals(request.getPrincipal()))
                .referenceFournisseur(request.getReferenceFournisseur())
                .delaiLivraisonJours(request.getDelaiLivraisonJours())
                .prixNegocie(request.getPrixNegocie())
                .build();

        ProductSupplier saved = productSupplierRepository.save(ps);
        product.getFournisseurs().add(saved);
        auditService.log("Product", product.getId(), AuditAction.AJOUT_FOURNISSEUR,
                "Fournisseur ajouté: " + supplier.getNom());
        return saved;
    }

    private void saveAttributes(Product product, Map<String, String> attributs) {
        attributs.forEach((code, valeur) -> {
            CustomAttributeDefinition def = attributeDefinitionRepository.findByCode(code)
                    .orElseThrow(() -> new BusinessException("Attribut inconnu: " + code));

            ProductCustomAttributeValue attrValue = ProductCustomAttributeValue.builder()
                    .product(product)
                    .attribute(def)
                    .valeur(valeur)
                    .build();
            attributeValueRepository.save(attrValue);
            product.getAttributs().add(attrValue);
        });
    }

    private BigDecimal getCurrentPrice(Product product, PriceType type) {
        return switch (type) {
            case ACHAT -> product.getPrixAchat();
            case VENTE -> product.getPrixVente();
            case PROMOTIONNEL -> product.getPrixPromotionnel();
        };
    }

    private void setPrice(Product product, PriceType type, BigDecimal price) {
        switch (type) {
            case ACHAT -> product.setPrixAchat(price);
            case VENTE -> product.setPrixVente(price);
            case PROMOTIONNEL -> product.setPrixPromotionnel(price);
        }
    }

    private void savePriceHistory(Product product, ProductVariant variant, PriceType type,
                                  BigDecimal oldPrice, BigDecimal newPrice, String utilisateur) {
        PriceHistory history = PriceHistory.builder()
                .product(product)
                .variant(variant)
                .type(type)
                .ancienPrix(oldPrice)
                .nouveauPrix(newPrice)
                .utilisateur(currentUserService.resolveActor(utilisateur))
                .build();
        priceHistoryRepository.save(history);
    }

    private String resolveProductSku(String requestedSku, String nom) {
        if (isNotBlank(requestedSku)) {
            String sku = requestedSku.trim();
            if (productRepository.existsBySku(sku)) {
                throw new BusinessException("SKU déjà existant: " + sku);
            }
            return sku;
        }
        String base = ProductSkuGenerator.baseFromProductName(nom);
        return ProductSkuGenerator.ensureUnique(base, productRepository::existsBySku);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
