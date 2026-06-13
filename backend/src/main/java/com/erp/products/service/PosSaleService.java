package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.*;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PosMapper;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PosSaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductPackagingRepository packagingRepository;
    private final LocationRepository locationRepository;
    private final StockLedgerService ledger;
    private final SettingsService settingsService;
    private final PosSessionService sessionService;
    private final CurrentUserService currentUserService;
    private final PosMapper mapper;
    private final AuditService auditService;

    @Transactional
    public SaleResponse createSale() {
        User cashier = currentUserService.requireCurrentUser();
        PosSession session = sessionService.requireOpenSession(cashier);
        Location location = resolveDefaultLocation(session.getWarehouse());

        Sale sale = Sale.builder()
                .saleNumber(generateSaleNumber())
                .posSession(session)
                .cashier(cashier)
                .warehouse(session.getWarehouse())
                .location(location)
                .status(SaleStatus.DRAFT)
                .lignes(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional(readOnly = true)
    public SaleResponse getSale(Long id) {
        return mapper.toSaleResponse(findSale(id));
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> listHoldSales() {
        User cashier = currentUserService.requireCurrentUser();
        PosSession session = sessionService.requireOpenSession(cashier);
        return saleRepository.findByPosSessionIdAndStatusOrderByCreatedAtDesc(session.getId(), SaleStatus.HOLD)
                .stream().map(mapper::toSaleResponse).toList();
    }

    @Transactional
    public SaleResponse upsertLine(Long saleId, SaleLineRequest request) {
        Sale sale = findSaleForUpdate(saleId);
        ensureEditable(sale);

        Product product = loadProduct(request.getProductId());
        ProductVariant variant = resolveVariant(product.getId(), request.getVariantId());
        ProductPackaging packaging = loadPackagingOptional(product.getId(), request.getPackagingId());

        BigDecimal qtyInput = request.getQuantityInput() != null
                ? request.getQuantityInput() : BigDecimal.ONE;
        if (qtyInput.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Quantite invalide");
        }

        BigDecimal qtyBase = resolveBaseQuantity(product, packaging, qtyInput);
        BigDecimal unitPrice = request.getUnitPrice() != null
                ? request.getUnitPrice() : resolveUnitPrice(product);
        BigDecimal taxRate = request.getTaxRate() != null
                ? request.getTaxRate()
                : settingsService.getDecimal(com.erp.products.settings.SettingKeys.POS_TAX_RATE_DEFAULT);
        if (taxRate == null) {
            taxRate = BigDecimal.ZERO;
        }
        BigDecimal discount = request.getDiscountAmount() != null
                ? request.getDiscountAmount() : BigDecimal.ZERO;

        Optional<SaleLine> existing = sale.getLignes().stream()
                .filter(l -> sameLine(l, product.getId(), variant, packaging))
                .findFirst();

        if (existing.isPresent()) {
            SaleLine line = existing.get();
            line.setQuantityInput(line.getQuantityInput().add(qtyInput));
            line.setQuantityInBaseUnit(line.getQuantityInBaseUnit().add(qtyBase));
            line.setLineTotal(computeLineTotal(line.getQuantityInput(), line.getUnitPrice(), line.getDiscountAmount()));
        } else {
            SaleLine line = SaleLine.builder()
                    .sale(sale)
                    .product(product)
                    .variant(variant)
                    .packaging(packaging)
                    .quantityInput(qtyInput)
                    .quantityInBaseUnit(qtyBase)
                    .unitPrice(unitPrice)
                    .discountAmount(discount)
                    .taxRate(taxRate)
                    .lineTotal(computeLineTotal(qtyInput, unitPrice, discount))
                    .build();
            sale.getLignes().add(line);
        }

        recalculateTotals(sale);
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponse updateLineQuantity(Long saleId, Long lineId, BigDecimal quantity) {
        Sale sale = findSaleForUpdate(saleId);
        ensureEditable(sale);
        SaleLine line = sale.getLignes().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ligne non trouvee: " + lineId));

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            sale.getLignes().remove(line);
        } else {
            line.setQuantityInput(quantity);
            line.setQuantityInBaseUnit(resolveBaseQuantity(line.getProduct(), line.getPackaging(), quantity));
            line.setLineTotal(computeLineTotal(line.getQuantityInput(), line.getUnitPrice(), line.getDiscountAmount()));
        }
        recalculateTotals(sale);
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponse applyLineDiscount(Long saleId, Long lineId, BigDecimal discountAmount) {
        Sale sale = findSaleForUpdate(saleId);
        ensureEditable(sale);
        SaleLine line = sale.getLignes().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ligne non trouvee: " + lineId));
        line.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        line.setLineTotal(computeLineTotal(line.getQuantityInput(), line.getUnitPrice(), line.getDiscountAmount()));
        recalculateTotals(sale);
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponse holdSale(Long saleId, String label) {
        Sale sale = findSaleForUpdate(saleId);
        if (sale.getStatus() != SaleStatus.DRAFT) {
            throw new BusinessException("Seule une vente brouillon peut etre mise en attente");
        }
        if (sale.getLignes().isEmpty()) {
            throw new BusinessException("Panier vide");
        }
        sale.setStatus(SaleStatus.HOLD);
        sale.setHoldLabel(label != null && !label.isBlank() ? label : "Vente en attente");
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponse resumeSale(Long saleId) {
        Sale sale = findSaleForUpdate(saleId);
        if (sale.getStatus() != SaleStatus.HOLD) {
            throw new BusinessException("Seule une vente en attente peut etre reprise");
        }
        sale.setStatus(SaleStatus.DRAFT);
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional
    public void deleteHoldSale(Long saleId) {
        Sale sale = findSaleForUpdate(saleId);
        if (sale.getStatus() != SaleStatus.HOLD) {
            throw new BusinessException("Seule une vente en attente peut etre supprimee");
        }
        saleRepository.delete(sale);
    }

    @Transactional
    public SaleResponse validateSale(Long saleId, SaleValidateRequest request) {
        Sale sale = findSaleForUpdate(saleId);
        if (sale.getStatus() != SaleStatus.DRAFT) {
            throw new BusinessException("Seule une vente brouillon peut etre validee");
        }
        if (sale.getLignes().isEmpty()) {
            throw new BusinessException("Panier vide");
        }
        sessionService.requireOpenSession(sale.getCashier());

        recalculateTotals(sale);
        BigDecimal total = sale.getTotal();
        List<SaleValidateRequest.PaymentInput> inputs = request.getPayments();
        if (inputs == null || inputs.isEmpty()) {
            throw new BusinessException("Paiement requis");
        }

        BigDecimal paid = inputs.stream()
                .map(SaleValidateRequest.PaymentInput::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(total) < 0) {
            throw new BusinessException("Montant paye insuffisant");
        }

        if (!settingsService.getStockConfig().isAllowNegativeStock()) {
            for (SaleLine line : sale.getLignes()) {
                BigDecimal available = ledger.getAvailable(
                        line.getProduct().getId(),
                        line.getVariant() != null ? line.getVariant().getId() : null,
                        sale.getWarehouse().getId());
                if (available.compareTo(line.getQuantityInBaseUnit()) < 0) {
                    throw new BusinessException("Stock insuffisant pour " + line.getProduct().getNom()
                            + " (disponible: " + available.stripTrailingZeros().toPlainString() + ")");
                }
            }
        }

        sale.getPayments().clear();
        for (SaleValidateRequest.PaymentInput input : inputs) {
            sale.getPayments().add(Payment.builder()
                    .sale(sale)
                    .method(input.getMethod())
                    .amount(input.getAmount())
                    .status(PaymentStatus.PAID)
                    .build());
        }

        BigDecimal change = BigDecimal.ZERO;
        if (request.getCashReceived() != null && request.getCashReceived().compareTo(BigDecimal.ZERO) > 0) {
            change = request.getCashReceived().subtract(total).max(BigDecimal.ZERO);
        } else {
            change = paid.subtract(total).max(BigDecimal.ZERO);
        }

        for (SaleLine line : sale.getLignes()) {
            postStockMovement(sale, line);
        }

        sale.setPaidAmount(paid);
        sale.setChangeAmount(change);
        sale.setStatus(SaleStatus.VALIDATED);
        sale.setValidatedAt(Instant.now());

        Sale saved = saleRepository.save(sale);
        auditService.log("Sale", saved.getId(), AuditAction.MODIFICATION,
                "Vente POS validee " + saved.getSaleNumber(), sale.getCashier().getEmail());
        return mapper.toSaleResponse(saved);
    }

    @Transactional
    public SaleResponse cancelSale(Long saleId) {
        Sale sale = findSaleForUpdate(saleId);
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new BusinessException("Vente deja annulee");
        }
        if (sale.getStatus() == SaleStatus.VALIDATED) {
            throw new BusinessException("Une vente validee doit etre remboursee, pas annulee");
        }
        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancelledAt(Instant.now());
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    private void postStockMovement(Sale sale, SaleLine line) {
        ledger.applyOnHandChange(
                line.getProduct().getId(),
                line.getVariant() != null ? line.getVariant().getId() : null,
                sale.getWarehouse().getId(),
                sale.getLocation().getId(),
                null,
                line.getQuantityInBaseUnit().negate(),
                new StockLedgerService.MovementMeta(
                        StockMovementType.OUT,
                        "POS_SALE",
                        sale.getSaleNumber(),
                        "Vente caisse " + sale.getSaleNumber(),
                        sale.getCashier().getEmail(),
                        null, null, null, null, null,
                        line.getPackaging() != null ? line.getPackaging().getId() : null,
                        sale.getId(),
                        null));
    }

    private void recalculateTotals(Sale sale) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (SaleLine line : sale.getLignes()) {
            BigDecimal lineSub = line.getUnitPrice().multiply(line.getQuantityInput());
            subtotal = subtotal.add(lineSub);
            discountTotal = discountTotal.add(line.getDiscountAmount() != null ? line.getDiscountAmount() : BigDecimal.ZERO);
            BigDecimal taxable = line.getLineTotal();
            if (line.getTaxRate() != null && line.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
                taxTotal = taxTotal.add(taxable.multiply(line.getTaxRate())
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            }
        }

        sale.setSubtotal(subtotal.setScale(4, RoundingMode.HALF_UP));
        sale.setDiscountTotal(discountTotal.setScale(4, RoundingMode.HALF_UP));
        sale.setTaxTotal(taxTotal.setScale(4, RoundingMode.HALF_UP));
        sale.setTotal(subtotal.subtract(discountTotal).add(taxTotal).setScale(4, RoundingMode.HALF_UP));
    }

    private BigDecimal computeLineTotal(BigDecimal qty, BigDecimal unitPrice, BigDecimal discount) {
        BigDecimal d = discount != null ? discount : BigDecimal.ZERO;
        return unitPrice.multiply(qty).subtract(d).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    static BigDecimal resolveUnitPrice(Product product) {
        Instant now = Instant.now();
        if (product.getPrixPromotionnel() != null
                && (product.getPrixPromotionnelDebut() == null || !now.isBefore(product.getPrixPromotionnelDebut()))
                && (product.getPrixPromotionnelFin() == null || !now.isAfter(product.getPrixPromotionnelFin()))) {
            return product.getPrixPromotionnel();
        }
        return product.getPrixVente() != null ? product.getPrixVente() : BigDecimal.ZERO;
    }

    private BigDecimal resolveBaseQuantity(Product product, ProductPackaging packaging, BigDecimal qtyInput) {
        if (packaging != null) {
            return qtyInput.multiply(packaging.getQuantiteBase()).setScale(6, RoundingMode.HALF_UP);
        }
        return qtyInput.setScale(6, RoundingMode.HALF_UP);
    }

    private boolean sameLine(SaleLine line, Long productId, ProductVariant variant, ProductPackaging packaging) {
        Long variantId = variant != null ? variant.getId() : null;
        Long packagingId = packaging != null ? packaging.getId() : null;
        Long lineVariant = line.getVariant() != null ? line.getVariant().getId() : null;
        Long linePackaging = line.getPackaging() != null ? line.getPackaging().getId() : null;
        return line.getProduct().getId().equals(productId)
                && Objects.equals(lineVariant, variantId)
                && Objects.equals(linePackaging, packagingId);
    }

    private String generateSaleNumber() {
        String prefix = settingsService.getNumberingConfig().getSalePrefix()
                + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long seq = saleRepository.countBySaleNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private Location resolveDefaultLocation(Warehouse warehouse) {
        return locationRepository.findByWarehouseIdAndCode(warehouse.getId(), "DEFAULT")
                .or(() -> locationRepository.findByWarehouseIdAndActifTrueOrderByCodeAsc(warehouse.getId())
                        .stream().findFirst())
                .orElseThrow(() -> new BusinessException("Aucun emplacement actif pour l entrepot"));
    }

    private void ensureEditable(Sale sale) {
        if (sale.getStatus() != SaleStatus.DRAFT) {
            throw new BusinessException("Seule une vente brouillon peut etre modifiee");
        }
    }

    private Sale findSale(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvee: " + id));
    }

    private Sale findSaleForUpdate(Long id) {
        return saleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvee: " + id));
    }

    private Product loadProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouve: " + id));
    }

    private ProductVariant resolveVariant(Long productId, Long variantId) {
        if (variantId != null) {
            ProductVariant v = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvee: " + variantId));
            if (!v.getProduct().getId().equals(productId)) {
                throw new BusinessException("Variante invalide");
            }
            return v;
        }
        List<ProductVariant> variants = variantRepository.findByProductId(productId);
        return variants.size() == 1 ? variants.get(0) : null;
    }

    private ProductPackaging loadPackagingOptional(Long productId, Long packagingId) {
        if (packagingId == null) {
            return null;
        }
        return packagingRepository.findByIdAndProductId(packagingId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Conditionnement non trouve"));
    }
}
