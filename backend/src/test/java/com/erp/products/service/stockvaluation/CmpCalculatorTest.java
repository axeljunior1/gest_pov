package com.erp.products.service.stockvaluation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CmpCalculatorTest {

    @Test
    void inboundUpdatesWeightedAverage() {
        CmpCalculator.CmpState state = CmpCalculator.CmpState.zero();

        CmpCalculator.CmpMovementResult first = CmpCalculator.applyInbound(state, bd("10"), bd("5"));
        assertBd("10", first.stateAfter().quantityOnHand());
        assertBd("50", first.stateAfter().stockValue());
        assertBd("5", first.stateAfter().averageUnitCost());

        CmpCalculator.CmpMovementResult second = CmpCalculator.applyInbound(first.stateAfter(), bd("10"), bd("7"));
        assertBd("20", second.stateAfter().quantityOnHand());
        assertBd("120", second.stateAfter().stockValue());
        assertBd("6", second.stateAfter().averageUnitCost());
    }

    @Test
    void outboundUsesCurrentAverageAndFreezesCost() {
        CmpCalculator.CmpState state = new CmpCalculator.CmpState(bd("20"), bd("120"), bd("6"));

        CmpCalculator.CmpMovementResult sale = CmpCalculator.applyOutbound(state, bd("5"));
        assertBd("6", sale.unitCostApplied());
        assertBd("-30", sale.totalValueMoved());
        assertBd("15", sale.stateAfter().quantityOnHand());
        assertBd("90", sale.stateAfter().stockValue());
        assertBd("6", sale.stateAfter().averageUnitCost());
    }

    @Test
    void outboundAtZeroQuantityResetsValueButKeepsLastAverage() {
        CmpCalculator.CmpState state = new CmpCalculator.CmpState(bd("5"), bd("25"), bd("5"));

        CmpCalculator.CmpMovementResult sale = CmpCalculator.applyOutbound(state, bd("5"));
        assertBd("0", sale.stateAfter().quantityOnHand());
        assertBd("0", sale.stateAfter().stockValue());
        assertBd("5", sale.stateAfter().averageUnitCost());
    }

    @Test
    void purchaseReversalUsesOriginalCostNotCurrentAverage() {
        CmpCalculator.CmpState state = new CmpCalculator.CmpState(bd("20"), bd("120"), bd("6"));

        CmpCalculator.CmpMovementResult reversal = CmpCalculator.applyPurchaseReversal(state, bd("10"), bd("5"));
        assertBd("5", reversal.unitCostApplied());
        assertBd("-50", reversal.totalValueMoved());
        assertBd("10", reversal.stateAfter().quantityOnHand());
        assertBd("70", reversal.stateAfter().stockValue());
        assertBd("7", reversal.stateAfter().averageUnitCost());
    }

    @Test
    void returnReintegratesAtOriginalSaleCost() {
        CmpCalculator.CmpState afterSale = CmpCalculator.applyOutbound(
                new CmpCalculator.CmpState(bd("10"), bd("50"), bd("5")),
                bd("4")).stateAfter();

        CmpCalculator.CmpMovementResult ret = CmpCalculator.applyInbound(afterSale, bd("2"), bd("5"));
        assertBd("8", ret.stateAfter().quantityOnHand());
        assertBd("40", ret.stateAfter().stockValue());
        assertBd("5", ret.stateAfter().averageUnitCost());
    }

    @Test
    void negativeStockKeepsComputedAverage() {
        CmpCalculator.CmpState state = new CmpCalculator.CmpState(bd("2"), bd("10"), bd("5"));

        CmpCalculator.CmpMovementResult sale = CmpCalculator.applyOutbound(state, bd("5"));
        assertBd("-3", sale.stateAfter().quantityOnHand());
        assertBd("-15", sale.stateAfter().stockValue());
        assertBd("5", sale.stateAfter().averageUnitCost());
    }

    @Test
    void rejectsInvalidQuantities() {
        assertThrows(IllegalArgumentException.class,
                () -> CmpCalculator.applyInbound(CmpCalculator.CmpState.zero(), bd("0"), bd("5")));
        assertThrows(IllegalArgumentException.class,
                () -> CmpCalculator.applyOutbound(CmpCalculator.CmpState.zero(), bd("-1")));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value).setScale(CmpCalculator.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static void assertBd(String expected, BigDecimal actual) {
        assertEquals(0, bd(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }
}
