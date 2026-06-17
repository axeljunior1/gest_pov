package com.erp.products.service.stockvaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculs CMP purs (testables sans Spring).
 */
public final class CmpCalculator {

    public static final int MONEY_SCALE = 6;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private CmpCalculator() {
    }

    public record CmpState(
            BigDecimal quantityOnHand,
            BigDecimal stockValue,
            BigDecimal averageUnitCost) {

        public static CmpState zero() {
            return new CmpState(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public record CmpMovementResult(
            CmpState stateAfter,
            BigDecimal unitCostApplied,
            BigDecimal totalValueMoved,
            BigDecimal signedQuantity) {
    }

    /** Entrée : achat, retour client, réception. */
    public static CmpMovementResult applyInbound(CmpState current, BigDecimal quantity, BigDecimal unitCost) {
        requirePositive(quantity, "Quantité entrée");
        requireNonNegative(unitCost, "Coût unitaire");

        BigDecimal valueIn = quantity.multiply(unitCost).setScale(MONEY_SCALE, ROUNDING);
        BigDecimal newQty = current.quantityOnHand().add(quantity);
        BigDecimal newValue = current.stockValue().add(valueIn);
        BigDecimal newAvg = divideSafe(newValue, newQty, current.averageUnitCost());

        return new CmpMovementResult(
                new CmpState(newQty, newValue, newAvg),
                unitCost,
                valueIn,
                quantity);
    }

    /** Sortie : vente, ajustement négatif — utilise le CMP courant. */
    public static CmpMovementResult applyOutbound(CmpState current, BigDecimal quantity) {
        requirePositive(quantity, "Quantité sortie");

        BigDecimal unitCost = current.averageUnitCost();
        if (unitCost.compareTo(BigDecimal.ZERO) == 0 && current.quantityOnHand().compareTo(BigDecimal.ZERO) > 0) {
            unitCost = divideSafe(current.stockValue(), current.quantityOnHand(), BigDecimal.ZERO);
        }

        BigDecimal valueOut = quantity.multiply(unitCost).setScale(MONEY_SCALE, ROUNDING);
        BigDecimal newQty = current.quantityOnHand().subtract(quantity);
        BigDecimal newValue = current.stockValue().subtract(valueOut);

        BigDecimal newAvg;
        if (newQty.compareTo(BigDecimal.ZERO) == 0) {
            newValue = BigDecimal.ZERO;
            newAvg = unitCost;
        } else if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            newAvg = divideSafe(newValue, newQty, unitCost);
        } else {
            newAvg = divideSafe(newValue, newQty, unitCost);
        }

        return new CmpMovementResult(
                new CmpState(newQty, newValue, newAvg),
                unitCost,
                valueOut.negate(),
                quantity.negate());
    }

    /** Annulation entrée : sortie au coût d'origine (pas au CMP actuel). */
    public static CmpMovementResult applyPurchaseReversal(CmpState current, BigDecimal quantity, BigDecimal originalUnitCost) {
        return applyOutboundAtFixedCost(current, quantity, originalUnitCost);
    }

    public static CmpMovementResult applyOutboundAtFixedCost(CmpState current, BigDecimal quantity, BigDecimal unitCost) {
        requirePositive(quantity, "Quantité");
        requireNonNegative(unitCost, "Coût unitaire");

        BigDecimal valueOut = quantity.multiply(unitCost).setScale(MONEY_SCALE, ROUNDING);
        BigDecimal newQty = current.quantityOnHand().subtract(quantity);
        BigDecimal newValue = current.stockValue().subtract(valueOut);
        BigDecimal newAvg = newQty.compareTo(BigDecimal.ZERO) == 0
                ? unitCost
                : divideSafe(newValue, newQty, unitCost);

        if (newQty.compareTo(BigDecimal.ZERO) == 0) {
            newValue = BigDecimal.ZERO;
        }

        return new CmpMovementResult(
                new CmpState(newQty, newValue, newAvg),
                unitCost,
                valueOut.negate(),
                quantity.negate());
    }

    private static BigDecimal divideSafe(BigDecimal numerator, BigDecimal denominator, BigDecimal fallback) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return fallback != null ? fallback : BigDecimal.ZERO;
        }
        return numerator.divide(denominator, MONEY_SCALE, ROUNDING);
    }

    private static void requirePositive(BigDecimal value, String label) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(label + " doit être strictement positive");
        }
    }

    private static void requireNonNegative(BigDecimal value, String label) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + " ne peut pas être négatif");
        }
    }
}
