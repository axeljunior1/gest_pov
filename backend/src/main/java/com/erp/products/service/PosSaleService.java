package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.*;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PosMapper;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.security.PermissionEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosSaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductPackagingRepository packagingRepository;
    private final LocationRepository locationRepository;
    private final StockLedgerService ledger;
    private final SettingsService settingsService;
    private final PosConfigService posConfigService;
    private final PosSessionService sessionService;
    private final CurrentUserService currentUserService;
    private final PosMapper mapper;
    private final AuditService auditService;
    private final CustomerRepository customerRepository;
    private final LoyaltyService loyaltyService;
    private final PaymentRepository paymentRepository;
    private final PermissionEvaluator permissionChecker;
    private final ProductVariantPolicyService variantPolicyService;
    private final StockExitService stockExitService;
    private final BarcodeLookupService barcodeLookupService;
    private final SaleCancellationService saleCancellationService;
    private final SaleEventService saleEventService;
    private final com.erp.products.service.stockvaluation.StockCmpValuationService cmpValuationService;
    private final ClientConfigurationService clientConfigurationService;

    @Transactional
    public SaleResponse createSale() {
        User seller = currentUserService.requireCurrentUser();
        PosSession session = sessionService.requireSessionForSaleCreation(seller);
        Location location = resolveDefaultLocation(session.getWarehouse());

        Sale sale = Sale.builder()
                .saleNumber(generateSaleNumber())
                .posSession(session)
                .seller(seller)
                .cashier(seller)
                .createdBy(seller)
                .updatedBy(seller)
                .warehouse(session.getWarehouse())
                .location(location)
                .status(SaleStatus.DRAFT)
                .lignes(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
        Sale saved = saleRepository.save(sale);
        saleEventService.record(saved, SaleEventType.CREATED, "Création vente");
        return mapper.toSaleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> listPendingPayments() {
        User user = currentUserService.requireCurrentUser();
        sessionService.requireOpenSession(user, PosSessionType.CASHIER);
        List<Sale> sales;
        if (posConfigService.isCentralCashier()) {
            sales = saleRepository.findByStatusOrderBySubmittedAtAsc(SaleStatus.PENDING_PAYMENT);
        } else {
            PosSession session = sessionService.requireOpenSession(user, PosSessionType.CASHIER);
            sales = saleRepository.findByStatusAndWarehouseIdOrderBySubmittedAtAsc(
                    SaleStatus.PENDING_PAYMENT, session.getWarehouse().getId());
        }
        return sales.stream().map(mapper::toSaleResponse).toList();
    }

    @Transactional
    public SaleResponse sendToPayment(Long saleId) {
        return submitForPayment(saleId);
    }

    @Transactional
    public SaleResponse submitForPayment(Long saleId) {
        if (!posConfigService.isCentralCashier()) {
            throw new BusinessException("Envoi en caisse disponible uniquement en mode caisse centrale");
        }
        Sale sale = findSaleForUpdate(saleId);
        ensureEditable(sale);
        if (sale.getLignes().isEmpty()) {
            throw new BusinessException("Panier vide");
        }
        recalculateTotals(sale);
        ensureSufficientStock(sale);
        clearOrphanPayments(sale);
        ensureSellerAndCashier(sale);
        sale.setStatus(SaleStatus.PENDING_PAYMENT);
        sale.setSubmittedAt(Instant.now());
        touchUpdatedBy(sale);
        Sale saved = saleRepository.save(sale);
        saleEventService.record(saved, SaleEventType.SENT_TO_CASHIER, "Envoi caisse");
        auditService.log("Sale", saved.getId(), AuditAction.MODIFICATION,
                "Vente transferee a la caisse (a encaisser) " + saved.getSaleNumber(),
                resolveSellerEmail(saved));
        return mapper.toSaleResponse(saved);
    }

    @Transactional
    public SaleResponse recallFromPayment(Long saleId) {
        if (!posConfigService.isCentralCashier()) {
            throw new BusinessException("Retour saisie disponible uniquement en mode caisse centrale");
        }
        Sale sale = findSaleForUpdate(saleId);
        if (sale.getStatus() != SaleStatus.PENDING_PAYMENT) {
            throw new BusinessException("Seule une vente en attente de paiement peut revenir en saisie");
        }
        assertCanRecallFromPayment(sale);
        sale.setStatus(SaleStatus.HOLD);
        sale.setHoldLabel("Retour caisse — " + sale.getSaleNumber());
        sale.setSubmittedAt(null);
        touchUpdatedBy(sale);
        Sale saved = saleRepository.save(sale);
        saleEventService.record(saved, SaleEventType.RECALLED_FROM_CASHIER, "Retour vendeur");
        auditService.log("Sale", saved.getId(), AuditAction.MODIFICATION,
                "Vente renvoyee en attente vendeur " + saved.getSaleNumber(),
                currentUserService.requireCurrentUser().getEmail());
        return mapper.toSaleResponse(saved);
    }

    @Transactional(readOnly = true)
    public SaleResponse getSale(Long id) {
        return mapper.toSaleResponse(findSale(id));
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> listHoldSales() {
        User user = currentUserService.requireCurrentUser();
        PosSession session = sessionService.requireSessionForSaleCreation(user);
        List<Sale> sales;
        if (posConfigService.isCentralCashier()) {
            sales = saleRepository.findBySellerIdAndWarehouseIdAndStatusOrderByCreatedAtDesc(
                    user.getId(), session.getWarehouse().getId(), SaleStatus.HOLD);
        } else {
            sales = saleRepository.findByPosSessionIdAndStatusOrderByCreatedAtDesc(
                    session.getId(), SaleStatus.HOLD);
        }
        return sales.stream().map(mapper::toSaleResponse).toList();
    }

    @Transactional(readOnly = true)
    public long countHoldSales() {
        return listHoldSales().size();
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> listDraftSales() {
        User user = currentUserService.requireCurrentUser();
        PosSession session = sessionService.requireSessionForSaleCreation(user);
        return saleRepository.findByPosSessionIdAndStatusOrderByCreatedAtDesc(session.getId(), SaleStatus.DRAFT)
                .stream()
                .filter(s -> !s.getLignes().isEmpty())
                .map(mapper::toSaleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> listCompletedSales(Boolean sessionOnly, Integer limit) {
        User user = currentUserService.requireCurrentUser();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean fullAccess = permissionChecker.has(auth, "pos.report.read");

        Long sessionId = null;
        if (Boolean.TRUE.equals(sessionOnly)) {
            PosSessionResponse current = sessionService.getCurrentSessionOrNull();
            if (current == null) {
                return List.of();
            }
            sessionId = current.getId();
        }

        Long sellerId = null;
        Long cashierId = null;
        Long userId = null;
        if (!fullAccess) {
            boolean canCollect = permissionChecker.has(auth, "pos.payment.collect")
                    || permissionChecker.has(auth, "pos.payment.validate");
            boolean canPrepare = permissionChecker.has(auth, "pos.sale.send_to_payment")
                    || permissionChecker.has(auth, "pos.sale.prepare")
                    || permissionChecker.has(auth, "pos.sale.create");
            if (canPrepare && !canCollect) {
                sellerId = user.getId();
            } else if (canCollect && !canPrepare) {
                cashierId = user.getId();
            } else {
                userId = user.getId();
            }
        }

        int max = limit != null && limit > 0 ? Math.min(limit, 200) : 50;
        return saleRepository.findCompletedSales(
                        SaleStatuses.COUNTED_FOR_REVENUE,
                        sessionId,
                        userId,
                        sellerId,
                        cashierId,
                        PageRequest.of(0, max))
                .stream()
                .map(mapper::toSaleResponse)
                .toList();
    }

    @Transactional
    public SaleResponse upsertLine(Long saleId, SaleLineRequest request) {
        Sale sale = findSaleForUpdate(saleId);
        ensureEditable(sale);

        Product product = loadProduct(request.getProductId());
        ProductVariant variant = variantPolicyService.resolveForSale(product, request.getVariantId());
        ProductPackaging packaging = resolvePackaging(product, variant, request.getPackagingId());

        BigDecimal qtyInput = request.getQuantityInput() != null
                ? request.getQuantityInput() : BigDecimal.ONE;
        if (qtyInput.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Quantite invalide");
        }

        BigDecimal qtyBase = resolveBaseQuantity(product, packaging, qtyInput);
        BigDecimal unitPrice = resolveLineUnitPrice(product, variant, packaging, request);
        BigDecimal taxRate = clientConfigurationService.resolveSaleLineTaxRate(request.getTaxRate());
        BigDecimal discount = request.getDiscountAmount() != null
                ? request.getDiscountAmount() : BigDecimal.ZERO;

        Optional<SaleLine> existing = sale.getLignes().stream()
                .filter(l -> sameLine(l, product.getId(), variant, packaging, unitPrice, discount))
                .findFirst();
        boolean lineUpdated = existing.isPresent();

        if (lineUpdated) {
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
                    .productNameSnapshot(product.getNom())
                    .variantNameSnapshot(variant != null ? variantPolicyService.buildVariantName(variant) : null)
                    .packagingNameSnapshot(packaging != null ? packaging.getNom() : null)
                    .packagingQuantitySnapshot(packaging != null ? packaging.getQuantiteBase() : null)
                    .quantityInput(qtyInput)
                    .quantityInBaseUnit(qtyBase)
                    .unitPrice(unitPrice)
                    .unitPriceSnapshot(unitPrice)
                    .discountAmount(discount)
                    .taxRate(taxRate)
                    .lineTotal(computeLineTotal(qtyInput, unitPrice, discount))
                    .build();
            sale.getLignes().add(line);
        }

        recalculateTotals(sale);
        touchUpdatedBy(sale);
        Sale saved = saleRepository.save(sale);
        saleEventService.record(saved, lineUpdated ? SaleEventType.LINE_UPDATED : SaleEventType.LINE_ADDED,
                product.getNom(), "Qté " + qtyInput, null);
        return mapper.toSaleResponse(saved);
    }

    private void touchUpdatedBy(Sale sale) {
        sale.setUpdatedBy(currentUserService.requireCurrentUser());
    }

    @Transactional
    public BarcodeScanResponse addScannedItem(Long saleId, BarcodeScanRequest request) {
        if (request == null || request.getCode() == null || request.getCode().isBlank()) {
            throw new BusinessException("Code-barres requis");
        }
        BarcodeScanConfig config = settingsService.getBarcodeScanConfig();
        if (!config.isScanEnabled()) {
            throw new BusinessException("Scan code-barres désactivé");
        }

        List<BarcodeLookupResult> matches = barcodeLookupService.lookupBarcode(request.getCode());
        if (matches.isEmpty()) {
            throw new BusinessException("Aucun produit trouvé pour ce code-barres");
        }
        if (matches.size() > 1) {
            log.warn("Code-barres ambigu {} : {} correspondances actives",
                    BarcodeRegistryService.normalize(request.getCode()), matches.size());
            throw new BusinessException(
                    "Plusieurs articles correspondent à ce code-barres. Contactez un administrateur.");
        }

        BarcodeLookupResult lookup = matches.get(0);
        BigDecimal qtyInput = request.getQuantityInput() != null
                ? request.getQuantityInput() : BigDecimal.ONE;

        SaleLineRequest lineRequest = new SaleLineRequest();
        lineRequest.setProductId(lookup.getProductId());
        lineRequest.setVariantId(lookup.getVariantId());
        lineRequest.setPackagingId(lookup.getPackagingId());
        lineRequest.setQuantityInput(qtyInput);

        SaleResponse sale = upsertLine(saleId, lineRequest);
        String priceLabel = lookup.getSalePrice() != null
                ? lookup.getSalePrice().stripTrailingZeros().toPlainString()
                : "0";
        String message = "Produit ajouté : " + lookup.getDisplayName() + " — " + priceLabel;

        return BarcodeScanResponse.builder()
                .sale(sale)
                .lookup(lookup)
                .message(message)
                .build();
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
        touchUpdatedBy(sale);
        Sale saved = saleRepository.save(sale);
        saleEventService.record(saved, SaleEventType.HOLD, "Mise en pause");
        return mapper.toSaleResponse(saved);
    }

    @Transactional
    public SaleResponse resumeSale(Long saleId) {
        Sale sale = findSaleForUpdate(saleId);
        if (sale.getStatus() != SaleStatus.HOLD) {
            throw new BusinessException("Seule une vente en attente peut etre reprise");
        }
        sale.setStatus(SaleStatus.DRAFT);
        touchUpdatedBy(sale);
        Sale saved = saleRepository.save(sale);
        saleEventService.record(saved, SaleEventType.RESUMED, "Reprise vente");
        return mapper.toSaleResponse(saved);
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
    public SaleResponse assignCustomer(Long saleId, Long customerId) {
        Sale sale = findSaleForUpdate(saleId);
        ensureEditable(sale);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouve: " + customerId));
        if (!Boolean.TRUE.equals(customer.getIsActive())) {
            throw new BusinessException("Client inactif");
        }
        loyaltyService.clearRedemptionFromSale(sale);
        sale.setCustomer(customer);
        recalculateTotals(sale);
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponse removeCustomer(Long saleId) {
        Sale sale = findSaleForUpdate(saleId);
        ensureEditable(sale);
        loyaltyService.clearRedemptionFromSale(sale);
        sale.setCustomer(null);
        recalculateTotals(sale);
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponse applyLoyaltyRedemption(Long saleId, LoyaltyRedeemRequest request) {
        Sale sale = findSaleForUpdate(saleId);
        ensureEditable(sale);
        if (request.getPoints() == null || request.getPoints() <= 0) {
            throw new BusinessException("Nombre de points invalide");
        }
        loyaltyService.applyRedemptionToSale(sale, request.getPoints());
        recalculateTotals(sale);
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponse clearLoyaltyRedemption(Long saleId) {
        Sale sale = findSaleForUpdate(saleId);
        ensureEditable(sale);
        loyaltyService.clearRedemptionFromSale(sale);
        recalculateTotals(sale);
        return mapper.toSaleResponse(saleRepository.save(sale));
    }

    @Transactional
    public SaleResponse validateSale(Long saleId, SaleValidateRequest request) {
        Sale sale = findSaleForUpdate(saleId);
        if (SaleStatuses.isPaid(sale.getStatus())) {
            throw new BusinessException("Cette vente est deja payee");
        }
        PosSession paymentSession = resolvePaymentSession(sale);
        User paymentCollector = currentUserService.requireCurrentUser();
        ensureSellerAndCashier(sale);
        assertValidatableStatus(sale);
        if (sale.getLignes().isEmpty()) {
            throw new BusinessException("Panier vide");
        }

        recalculateTotals(sale);
        BigDecimal total = sale.getTotal();
        List<SaleValidateRequest.PaymentInput> inputs = request.getPayments();
        if (inputs == null || inputs.isEmpty()) {
            throw new BusinessException("Paiement requis");
        }

        PosConfigResponse config = posConfigService.getConfig();
        if (!config.isAllowSplitPayment() && inputs.size() > 1) {
            throw new BusinessException("Paiement fractionne desactive par parametre");
        }

        BigDecimal paid = inputs.stream()
                .map(SaleValidateRequest.PaymentInput::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(total) < 0) {
            if (!config.isAllowPartialPayment()) {
                throw new BusinessException("Montant paye insuffisant");
            }
        }

        if (!settingsService.getStockConfig().isAllowNegativeStock()) {
            ensureSufficientStock(sale);
        }

        for (SaleValidateRequest.PaymentInput input : inputs) {
            if (input.getMethod() == null || !clientConfigurationService.isPaymentMethodEnabled(input.getMethod().name())) {
                throw new BusinessException("Moyen de paiement non autorise: " + (input.getMethod() != null ? input.getMethod() : "?"));
            }
        }

        attachPayments(sale, paymentCollector, paymentSession, inputs);

        BigDecimal change;
        if (settingsService.getBoolean(com.erp.products.settings.SettingKeys.POS_CHANGE_GIVING_ENABLED)
                && request.getCashReceived() != null && request.getCashReceived().compareTo(BigDecimal.ZERO) > 0) {
            change = request.getCashReceived().subtract(total).max(BigDecimal.ZERO);
        } else {
            change = paid.subtract(total).max(BigDecimal.ZERO);
        }

        StockExit stockExit = stockExitService.createFromPosSale(sale);

        Instant paidAt = Instant.now();
        for (SaleLine line : sale.getLignes()) {
            Long variantId = line.getVariant() != null ? line.getVariant().getId() : null;
            BigDecimal unitCost = cmpValuationService.recordSale(
                    line.getProduct().getId(),
                    variantId,
                    line.getQuantityInBaseUnit(),
                    paidAt,
                    sale.getId(),
                    "SALE");
            line.setUnitCostAtSale(unitCost);
            postStockMovement(sale, line, stockExit.getId());
        }

        loyaltyService.processSaleValidated(sale);

        sale.setPaymentSession(paymentSession);
        sale.setCashier(paymentCollector);
        sale.setPaidAmount(paid);
        sale.setChangeAmount(change);
        sale.setStatus(SaleStatus.PAID);
        sale.setValidatedAt(paidAt);
        sale.setPaidAt(paidAt);

        Sale saved = saleRepository.save(sale);
        String paymentDetails = inputs.stream()
                .map(p -> p.getMethod().name() + " " + p.getAmount().setScale(2, RoundingMode.HALF_UP))
                .collect(Collectors.joining(", "));
        saleEventService.record(
                saved,
                SaleEventType.PAYMENT_VALIDATED,
                "Encaissement — " + paid.setScale(2, RoundingMode.HALF_UP),
                paymentDetails,
                paymentCollector);
        auditService.log("Sale", saved.getId(), AuditAction.MODIFICATION,
                "Vente POS validee " + saved.getSaleNumber(), currentUserService.requireCurrentUser().getEmail());
        return mapper.toSaleResponse(saved);
    }

    private PosSession resolvePaymentSession(Sale sale) {
        if (posConfigService.isCentralCashier()) {
            User cashier = currentUserService.requireCurrentUser();
            return sessionService.requireOpenSession(cashier, PosSessionType.CASHIER);
        }
        return sessionService.requireOpenSession(sale.getCashier(), PosSessionType.CASHIER);
    }

    private void assertValidatableStatus(Sale sale) {
        if (posConfigService.isCentralCashier()) {
            if (sale.getStatus() != SaleStatus.PENDING_PAYMENT) {
                throw new BusinessException("Seule une vente en attente de paiement peut etre encaissee");
            }
            return;
        }
        if (sale.getStatus() != SaleStatus.DRAFT) {
            throw new BusinessException("Seule une vente brouillon peut etre validee");
        }
    }

    @Transactional
    public SaleResponse cancelSale(Long saleId, CancelSaleRequest request) {
        Sale sale = findSaleForUpdate(saleId);
        return saleCancellationService.cancel(sale, request, null, false);
    }

    private void postStockMovement(Sale sale, SaleLine line, Long stockExitId) {
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
                        resolveSellerEmail(sale),
                        null, null, null, null, stockExitId,
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
        BigDecimal loyaltyDiscount = sale.getLoyaltyDiscountAmount() != null
                ? sale.getLoyaltyDiscountAmount() : BigDecimal.ZERO;
        sale.setTotal(subtotal.subtract(discountTotal).subtract(loyaltyDiscount)
                .add(taxTotal).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP));
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

    private void ensureSufficientStock(Sale sale) {
        if (settingsService.getStockConfig().isAllowNegativeStock()) {
            return;
        }
        for (SaleLine line : sale.getLignes()) {
            variantPolicyService.resolveForSale(line.getProduct(),
                    line.getVariant() != null ? line.getVariant().getId() : null);
            BigDecimal available = ledger.getAvailable(
                    line.getProduct().getId(),
                    line.getVariant() != null ? line.getVariant().getId() : null,
                    sale.getWarehouse().getId());
            if (available.compareTo(line.getQuantityInBaseUnit()) < 0) {
                throw new BusinessException(buildStockInsufficientMessage(line, available));
            }
        }
    }

    private String buildStockInsufficientMessage(SaleLine line, BigDecimal available) {
        String label = line.getVariantNameSnapshot() != null
                ? line.getProductNameSnapshot() + " — " + line.getVariantNameSnapshot()
                : (line.getProductNameSnapshot() != null ? line.getProductNameSnapshot() : line.getProduct().getNom());
        return "Stock insuffisant pour " + label
                + " (disponible: " + available.stripTrailingZeros().toPlainString() + ")";
    }

    private BigDecimal resolveLineUnitPrice(
            Product product,
            ProductVariant variant,
            ProductPackaging packaging,
            SaleLineRequest request) {
        if (packaging != null) {
            BigDecimal packagingPrice = packaging.getPrixVente();
            if (request.getUnitPrice() != null
                    && request.getUnitPrice().compareTo(packagingPrice) != 0
                    && canUpdatePackagingPrice()) {
                return request.getUnitPrice().setScale(4, RoundingMode.HALF_UP);
            }
            return packagingPrice;
        }
        if (request.getUnitPrice() != null && canUpdatePackagingPrice()) {
            return request.getUnitPrice().setScale(4, RoundingMode.HALF_UP);
        }
        if (variant != null && variant.getPrix() != null) {
            return variant.getPrix();
        }
        return resolveUnitPrice(product);
    }

    private ProductPackaging resolvePackaging(Product product, ProductVariant variant, Long packagingId) {
        if (packagingId != null) {
            ProductPackaging packaging = loadPackagingOptional(product.getId(), packagingId);
            if (!Boolean.TRUE.equals(packaging.getActif())) {
                throw new BusinessException("Conditionnement inactif");
            }
            PackagingService.assertUsableForSale(packaging);
            if (packaging.getVariant() != null && variant != null
                    && !packaging.getVariant().getId().equals(variant.getId())) {
                throw new BusinessException("Conditionnement invalide pour cette variante");
            }
            return packaging;
        }
        Long variantId = variant != null ? variant.getId() : null;
        return packagingRepository.findByProductIdAndActifTrueAndUsableForSaleTrueOrderByNomAsc(product.getId()).stream()
                .filter(p -> p.getVariant() == null
                        || (variantId != null && p.getVariant().getId().equals(variantId)))
                .filter(p -> Boolean.TRUE.equals(p.getDefaultVente()))
                .findFirst()
                .or(() -> packagingRepository.findByProductIdAndActifTrueAndUsableForSaleTrueOrderByNomAsc(product.getId()).stream()
                        .filter(p -> p.getVariant() == null
                                || (variantId != null && p.getVariant().getId().equals(variantId)))
                        .findFirst())
                .orElse(null);
    }

    private boolean canUpdatePackagingPrice() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return permissionChecker.has(auth, "product_packaging.update_price");
    }

    private boolean sameLine(
            SaleLine line,
            Long productId,
            ProductVariant variant,
            ProductPackaging packaging,
            BigDecimal unitPrice,
            BigDecimal discount) {
        Long variantId = variant != null ? variant.getId() : null;
        Long packagingId = packaging != null ? packaging.getId() : null;
        Long lineVariant = line.getVariant() != null ? line.getVariant().getId() : null;
        Long linePackaging = line.getPackaging() != null ? line.getPackaging().getId() : null;
        BigDecimal lineDiscount = line.getDiscountAmount() != null ? line.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal expectedDiscount = discount != null ? discount : BigDecimal.ZERO;
        return line.getProduct().getId().equals(productId)
                && Objects.equals(lineVariant, variantId)
                && Objects.equals(linePackaging, packagingId)
                && line.getUnitPrice().compareTo(unitPrice) == 0
                && lineDiscount.compareTo(expectedDiscount) == 0;
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

    private void attachPayments(
            Sale sale,
            User collector,
            PosSession session,
            List<SaleValidateRequest.PaymentInput> inputs) {
        if (!sale.getPayments().isEmpty()) {
            paymentRepository.deleteBySaleId(sale.getId());
            sale.getPayments().clear();
        }
        for (SaleValidateRequest.PaymentInput input : inputs) {
            Payment payment = Payment.builder()
                    .sale(sale)
                    .cashier(collector)
                    .posSession(session)
                    .method(input.getMethod())
                    .amount(input.getAmount())
                    .status(PaymentStatus.PAID)
                    .build();
            sale.getPayments().add(payment);
        }
    }

    private void clearOrphanPayments(Sale sale) {
        paymentRepository.deleteBySaleId(sale.getId());
        if (sale.getPayments() != null) {
            sale.getPayments().clear();
        }
    }

    private void ensureSellerAndCashier(Sale sale) {
        if (sale.getSeller() == null && sale.getCashier() != null) {
            sale.setSeller(sale.getCashier());
        } else if (sale.getSeller() == null) {
            sale.setSeller(currentUserService.requireCurrentUser());
        }
        if (sale.getCashier() == null) {
            sale.setCashier(sale.getSeller());
        }
    }

    private String resolveSellerEmail(Sale sale) {
        if (sale.getSeller() != null) {
            return sale.getSeller().getEmail();
        }
        return sale.getCashier().getEmail();
    }

    private void ensureEditable(Sale sale) {
        if (sale.getStatus() != SaleStatus.DRAFT) {
            throw new BusinessException("Seule une vente brouillon peut etre modifiee");
        }
    }

    private void assertCanRecallFromPayment(Sale sale) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (permissionChecker.has(auth, "pos.payment.collect")) {
            return;
        }
        User user = currentUserService.requireCurrentUser();
        boolean canPrepare = permissionChecker.has(auth, "pos.sale.send_to_payment")
                || permissionChecker.has(auth, "pos.sale.create")
                || permissionChecker.has(auth, "pos.sale.prepare");
        if (canPrepare && sale.getSeller() != null && sale.getSeller().getId().equals(user.getId())) {
            return;
        }
        throw new BusinessException("Droit insuffisant pour renvoyer cette vente en saisie");
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

    private ProductVariant resolveVariant(Product product, Long variantId) {
        return variantPolicyService.resolveForSale(product, variantId);
    }

    private ProductPackaging loadPackagingOptional(Long productId, Long packagingId) {
        if (packagingId == null) {
            return null;
        }
        ProductPackaging packaging = packagingRepository.findByIdAndProductId(packagingId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Conditionnement non trouve"));
        PackagingService.assertUsableForSale(packaging);
        return packaging;
    }
}
