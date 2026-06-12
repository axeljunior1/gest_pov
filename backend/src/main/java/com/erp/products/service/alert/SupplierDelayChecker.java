package com.erp.products.service.alert;

import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.domain.enums.PurchaseOrderStatus;
import com.erp.products.repository.SupplierPurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SupplierDelayChecker {

    private final SupplierPurchaseOrderRepository orderRepository;
    private final AlertService alertService;

    public void checkAll() {
        LocalDate today = LocalDate.now();
        orderRepository.findByStatusAndExpectedDeliveryDateBefore(PurchaseOrderStatus.PENDING, today)
                .forEach(order -> {
                    AlertService.AlertPosition position = new AlertService.AlertPosition(
                            order.getProduct(), null, null, null);

                    alertService.triggerIfNeeded(
                            AlertType.SUPPLIER_DELAY,
                            AlertSeverity.WARNING,
                            position,
                            BigDecimal.valueOf(today.toEpochDay() - order.getExpectedDeliveryDate().toEpochDay()),
                            null,
                            "Retard fournisseur " + order.getSupplier().getNom()
                                    + " — commande " + order.getReference()
                                    + " (prevue le " + order.getExpectedDeliveryDate() + ")");
                });
    }
}
