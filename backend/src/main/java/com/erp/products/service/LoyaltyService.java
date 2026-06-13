package com.erp.products.service;

import com.erp.products.domain.entity.Customer;
import com.erp.products.domain.entity.LoyaltyTransaction;
import com.erp.products.domain.entity.Sale;
import com.erp.products.domain.entity.SaleLine;
import com.erp.products.domain.enums.LoyaltyTransactionType;
import com.erp.products.dto.LoyaltyConfigResponse;
import com.erp.products.dto.LoyaltyTransactionResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.CustomerRepository;
import com.erp.products.repository.LoyaltyTransactionRepository;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltySettingsService loyaltySettingsService;
    private final LoyaltyTransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponse> listTransactions(Long customerId) {
        return transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void applyRedemptionToSale(Sale sale, int points) {
        if (sale.getCustomer() == null) {
            throw new BusinessException("Client requis pour utiliser les points");
        }
        LoyaltyConfigResponse config = loyaltySettingsService.getConfig();
        if (!config.isLoyaltyEnabled()) {
            throw new BusinessException("Fidelite desactivee");
        }
        if (!config.isAllowPointsRedemption()) {
            throw new BusinessException("Utilisation des points desactivee");
        }
        if (points <= 0) {
            throw new BusinessException("Nombre de points invalide");
        }
        Integer minimum = config.getMinimumPointsToRedeem();
        if (minimum != null && points < minimum) {
            throw new BusinessException("Minimum " + minimum + " points requis");
        }
        Customer customer = sale.getCustomer();
        if (customer.getLoyaltyPoints() < points) {
            throw new BusinessException("Solde points insuffisant");
        }

        BigDecimal pointValue = config.getPointValue() != null ? config.getPointValue() : BigDecimal.ZERO;
        BigDecimal discount = pointValue.multiply(BigDecimal.valueOf(points)).setScale(4, RoundingMode.HALF_UP);

        BigDecimal baseTotal = computeBaseTotalBeforeLoyalty(sale);
        BigDecimal maxPercent = config.getMaximumDiscountPercent() != null
                ? config.getMaximumDiscountPercent() : BigDecimal.valueOf(100);
        BigDecimal maxDiscount = baseTotal.multiply(maxPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (discount.compareTo(maxDiscount) > 0) {
            throw new BusinessException("Remise max autorisee : " + maxDiscount.stripTrailingZeros().toPlainString());
        }
        if (discount.compareTo(baseTotal) > 0) {
            discount = baseTotal;
            points = discount.divide(pointValue, 0, RoundingMode.FLOOR).intValue();
        }

        sale.setLoyaltyPointsRedeemed(points);
        sale.setLoyaltyDiscountAmount(discount);
    }

    @Transactional
    public void clearRedemptionFromSale(Sale sale) {
        sale.setLoyaltyPointsRedeemed(0);
        sale.setLoyaltyDiscountAmount(BigDecimal.ZERO);
    }

    @Transactional
    public void processSaleValidated(Sale sale) {
        Customer customer = sale.getCustomer();
        if (customer == null) {
            return;
        }
        LoyaltyConfigResponse config = loyaltySettingsService.getConfig();
        if (!config.isLoyaltyEnabled()) {
            return;
        }

        String snapshot = loyaltySettingsService.snapshotRules();
        String actor = currentUserService.getCurrentUserEmailOrDefault();

        int redeemed = sale.getLoyaltyPointsRedeemed() != null ? sale.getLoyaltyPointsRedeemed() : 0;
        if (redeemed > 0) {
            int before = customer.getLoyaltyPoints();
            if (before < redeemed) {
                throw new BusinessException("Solde points insuffisant au paiement");
            }
            customer.setLoyaltyPoints(before - redeemed);
            transactionRepository.save(LoyaltyTransaction.builder()
                    .customer(customer)
                    .sale(sale)
                    .type(LoyaltyTransactionType.REDEEM)
                    .points(-redeemed)
                    .amountValue(sale.getLoyaltyDiscountAmount())
                    .balanceBefore(before)
                    .balanceAfter(customer.getLoyaltyPoints())
                    .ruleSnapshot(snapshot)
                    .createdBy(actor)
                    .build());
        }

        int earned = calculateEarnPoints(sale, config);
        sale.setLoyaltyPointsEarned(earned);
        if (earned > 0) {
            int before = customer.getLoyaltyPoints();
            customer.setLoyaltyPoints(before + earned);
            transactionRepository.save(LoyaltyTransaction.builder()
                    .customer(customer)
                    .sale(sale)
                    .type(LoyaltyTransactionType.EARN)
                    .points(earned)
                    .amountValue(sale.getTotal())
                    .balanceBefore(before)
                    .balanceAfter(customer.getLoyaltyPoints())
                    .ruleSnapshot(snapshot)
                    .createdBy(actor)
                    .build());
        }

        customer.setLoyaltyTier(loyaltySettingsService.resolveTier(
                customer.getLoyaltyPoints(), config.getLoyaltyTiersConfig()));
        customerRepository.save(customer);
    }

    @Transactional
    public void processRefund(Sale sale, BigDecimal refundAmount) {
        Customer customer = sale.getCustomer();
        if (customer == null) {
            return;
        }
        LoyaltyConfigResponse config = loyaltySettingsService.getConfig();
        if (!config.isLoyaltyEnabled()) {
            return;
        }

        BigDecimal saleTotal = sale.getTotal();
        if (saleTotal == null || saleTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal ratio = refundAmount.divide(saleTotal, 6, RoundingMode.HALF_UP).min(BigDecimal.ONE);

        int earned = sale.getLoyaltyPointsEarned() != null ? sale.getLoyaltyPointsEarned() : 0;
        int redeemed = sale.getLoyaltyPointsRedeemed() != null ? sale.getLoyaltyPointsRedeemed() : 0;
        int earnReversal = BigDecimal.valueOf(earned).multiply(ratio).setScale(0, RoundingMode.FLOOR).intValue();
        int redeemRestore = BigDecimal.valueOf(redeemed).multiply(ratio).setScale(0, RoundingMode.FLOOR).intValue();

        String snapshot = loyaltySettingsService.snapshotRules();
        String actor = currentUserService.getCurrentUserEmailOrDefault();

        if (earnReversal > 0) {
            int before = customer.getLoyaltyPoints();
            customer.setLoyaltyPoints(Math.max(0, before - earnReversal));
            transactionRepository.save(LoyaltyTransaction.builder()
                    .customer(customer)
                    .sale(sale)
                    .type(LoyaltyTransactionType.REFUND_REVERSAL)
                    .points(-earnReversal)
                    .balanceBefore(before)
                    .balanceAfter(customer.getLoyaltyPoints())
                    .ruleSnapshot(snapshot)
                    .createdBy(actor)
                    .build());
        }
        if (redeemRestore > 0) {
            int before = customer.getLoyaltyPoints();
            customer.setLoyaltyPoints(before + redeemRestore);
            transactionRepository.save(LoyaltyTransaction.builder()
                    .customer(customer)
                    .sale(sale)
                    .type(LoyaltyTransactionType.REFUND_REVERSAL)
                    .points(redeemRestore)
                    .balanceBefore(before)
                    .balanceAfter(customer.getLoyaltyPoints())
                    .ruleSnapshot(snapshot)
                    .createdBy(actor)
                    .build());
        }

        customer.setLoyaltyTier(loyaltySettingsService.resolveTier(
                customer.getLoyaltyPoints(), config.getLoyaltyTiersConfig()));
        customerRepository.save(customer);
    }

    @Transactional
    public Customer manualAdjust(Long customerId, int points, String reason, boolean add) {
        if (points <= 0) {
            throw new BusinessException("Nombre de points invalide");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouve: " + customerId));
        LoyaltyConfigResponse config = loyaltySettingsService.getConfig();
        String snapshot = loyaltySettingsService.snapshotRules();
        String actor = currentUserService.getCurrentUserEmailOrDefault();
        int before = customer.getLoyaltyPoints();

        if (add) {
            customer.setLoyaltyPoints(before + points);
            transactionRepository.save(LoyaltyTransaction.builder()
                    .customer(customer)
                    .type(LoyaltyTransactionType.MANUAL_ADD)
                    .points(points)
                    .balanceBefore(before)
                    .balanceAfter(customer.getLoyaltyPoints())
                    .ruleSnapshot(snapshot)
                    .createdBy(actor + (reason != null ? " — " + reason : ""))
                    .build());
        } else {
            if (before < points) {
                throw new BusinessException("Solde insuffisant");
            }
            customer.setLoyaltyPoints(before - points);
            transactionRepository.save(LoyaltyTransaction.builder()
                    .customer(customer)
                    .type(LoyaltyTransactionType.MANUAL_REMOVE)
                    .points(-points)
                    .balanceBefore(before)
                    .balanceAfter(customer.getLoyaltyPoints())
                    .ruleSnapshot(snapshot)
                    .createdBy(actor + (reason != null ? " — " + reason : ""))
                    .build());
        }

        customer.setLoyaltyTier(loyaltySettingsService.resolveTier(
                customer.getLoyaltyPoints(), config.getLoyaltyTiersConfig()));
        return customerRepository.save(customer);
    }

    public int calculateEarnPoints(Sale sale, LoyaltyConfigResponse config) {
        BigDecimal eligible = computeEligibleEarnAmount(sale, config);
        BigDecimal unitAmount = config.getCurrencyUnitAmount();
        BigDecimal pointsPerUnit = config.getPointsPerCurrencyUnit();
        if (unitAmount == null || unitAmount.compareTo(BigDecimal.ZERO) <= 0
                || pointsPerUnit == null || pointsPerUnit.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return eligible.divide(unitAmount, 0, RoundingMode.FLOOR)
                .multiply(pointsPerUnit)
                .intValue();
    }

    private BigDecimal computeEligibleEarnAmount(Sale sale, LoyaltyConfigResponse config) {
        if (!config.isEarnPointsOnDiscountedSales()
                && sale.getDiscountTotal() != null
                && sale.getDiscountTotal().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal sum = BigDecimal.ZERO;
            for (SaleLine line : sale.getLignes()) {
                sum = sum.add(line.getUnitPrice().multiply(line.getQuantityInput()));
            }
            return sum.max(BigDecimal.ZERO);
        }
        BigDecimal net = sale.getSubtotal().subtract(
                sale.getDiscountTotal() != null ? sale.getDiscountTotal() : BigDecimal.ZERO);
        if (config.isEarnPointsOnTaxIncludedAmount()) {
            net = net.add(sale.getTaxTotal() != null ? sale.getTaxTotal() : BigDecimal.ZERO);
        }
        net = net.subtract(sale.getLoyaltyDiscountAmount() != null ? sale.getLoyaltyDiscountAmount() : BigDecimal.ZERO);
        return net.max(BigDecimal.ZERO);
    }

    private BigDecimal computeBaseTotalBeforeLoyalty(Sale sale) {
        BigDecimal sub = sale.getSubtotal() != null ? sale.getSubtotal() : BigDecimal.ZERO;
        BigDecimal disc = sale.getDiscountTotal() != null ? sale.getDiscountTotal() : BigDecimal.ZERO;
        BigDecimal tax = sale.getTaxTotal() != null ? sale.getTaxTotal() : BigDecimal.ZERO;
        return sub.subtract(disc).add(tax).max(BigDecimal.ZERO);
    }

    private LoyaltyTransactionResponse toResponse(LoyaltyTransaction tx) {
        return LoyaltyTransactionResponse.builder()
                .id(tx.getId())
                .customerId(tx.getCustomer().getId())
                .saleId(tx.getSale() != null ? tx.getSale().getId() : null)
                .saleNumber(tx.getSale() != null ? tx.getSale().getSaleNumber() : null)
                .type(tx.getType())
                .points(tx.getPoints())
                .amountValue(tx.getAmountValue())
                .balanceBefore(tx.getBalanceBefore())
                .balanceAfter(tx.getBalanceAfter())
                .createdBy(tx.getCreatedBy())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
