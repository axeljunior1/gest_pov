package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.*;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.ProductMapper;
import com.erp.products.repository.*;
import com.erp.products.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    private final ProductMapper mapper;
    private final AuditService auditService;
    private final BarcodeService barcodeService;
    private final FileStorageService fileStorageService;

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
        Product product = productRepository.findByVariantBarcode(codeBarre)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé pour le code-barres: " + codeBarre));
        return mapper.toProductResponse(product, barcodeService, true);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("SKU déjà existant: " + request.getSku());
        }

        Product product = Product.builder()
                .nom(request.getNom())
                .sku(request.getSku())
                .description(request.getDescription())
                .marque(request.getMarque())
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
            request.getVariantes().forEach(v -> addVariantInternal(saved, v, request.getUtilisateur()));
        }

        if (request.getFournisseurs() != null) {
            request.getFournisseurs().forEach(f -> addSupplierInternal(saved, f));
        }

        if (request.getAttributs() != null) {
            saveAttributes(saved, request.getAttributs());
        }

        auditService.log("Product", saved.getId(), AuditAction.CREATION,
                "Produit créé: " + saved.getNom(), request.getUtilisateur());

        return mapper.toProductResponse(findProduct(saved.getId()), barcodeService, true);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("SKU déjà existant: " + request.getSku());
        }

        product.setNom(request.getNom());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setMarque(request.getMarque());
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
        product.setCycleVie(request.getCycleVie());
        Product saved = productRepository.save(product);

        auditService.log("Product", saved.getId(), AuditAction.CHANGEMENT_CYCLE_VIE,
                "Cycle de vie: " + old + " -> " + request.getCycleVie(), request.getUtilisateur());

        return mapper.toProductResponse(findProduct(saved.getId()), barcodeService, true);
    }

    @Transactional
    public ProductResponse updatePrice(Long id, PriceUpdateRequest request) {
        Product product = findProduct(id);
        BigDecimal oldPrice = getCurrentPrice(product, request.getType());
        setPrice(product, request.getType(), request.getNouveauPrix());
        Product saved = productRepository.save(product);

        savePriceHistory(saved, null, request.getType(), oldPrice, request.getNouveauPrix(), request.getUtilisateur());
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
        ProductVariant variant = addVariantInternal(product, request, "system");
        return mapper.toVariantResponse(variant, barcodeService);
    }

    @Transactional
    public ProductVariantResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request) {
        ProductVariant variant = findVariant(productId, variantId);

        if (!variant.getSku().equals(request.getSku()) && variantRepository.existsBySku(request.getSku())) {
            throw new BusinessException("SKU variante déjà existant: " + request.getSku());
        }

        variant.setCouleur(request.getCouleur());
        variant.setTaille(request.getTaille());
        variant.setSku(request.getSku());
        variant.setPrix(request.getPrix());
        variant.setStock(request.getStock());
        applyBarcode(variant, request);

        return mapper.toVariantResponse(variantRepository.save(variant), barcodeService);
    }

    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant variant = findVariant(productId, variantId);
        priceHistoryRepository.deleteByVariantId(variantId);
        variantRepository.delete(variant);
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

    private ProductVariant addVariantInternal(Product product, ProductVariantRequest request, String utilisateur) {
        if (variantRepository.existsBySku(request.getSku())) {
            throw new BusinessException("SKU variante déjà existant: " + request.getSku());
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .couleur(request.getCouleur())
                .taille(request.getTaille())
                .sku(request.getSku())
                .prix(request.getPrix())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .build();

        applyBarcode(variant, request);
        ProductVariant saved = variantRepository.save(variant);
        product.getVariantes().add(saved);
        auditService.log("Product", product.getId(), AuditAction.AJOUT_VARIANTE,
                "Variante ajoutée: " + saved.getSku(), utilisateur);
        return saved;
    }

    private void applyBarcode(ProductVariant variant, ProductVariantRequest request) {
        if (Boolean.TRUE.equals(request.getGenerateBarcode())) {
            BarcodeType type = request.getBarcodeType() != null ? request.getBarcodeType() : BarcodeType.CODE128;
            String content = request.getCodeBarre() != null ? request.getCodeBarre() : variant.getSku();
            variant.setCodeBarre(content);
            variant.setBarcodeType(type);
        } else if (request.getCodeBarre() != null) {
            variant.setCodeBarre(request.getCodeBarre());
            variant.setBarcodeType(request.getBarcodeType());
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
                .utilisateur(utilisateur != null ? utilisateur : "system")
                .build();
        priceHistoryRepository.save(history);
    }
}
