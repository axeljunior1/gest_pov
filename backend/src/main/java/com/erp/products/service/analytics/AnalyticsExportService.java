package com.erp.products.service.analytics;

import com.erp.products.dto.analytics.AnalyticsFilterRequest;
import com.erp.products.dto.analytics.AnalyticsProductRow;
import com.erp.products.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsExportService {

    private final ProductAnalyticsService productAnalyticsService;
    private final PaymentAnalyticsService paymentAnalyticsService;
    private final CashierAnalyticsService cashierAnalyticsService;

    public byte[] exportCsv(AnalyticsFilterRequest filter, String type) {
        String csv = switch (type.toLowerCase()) {
            case "products" -> exportProducts(filter);
            case "payments" -> exportPayments(filter);
            case "cashiers" -> exportCashiers(filter);
            default -> throw new BusinessException("Type d'export inconnu: " + type);
        };
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    private String exportProducts(AnalyticsFilterRequest filter) {
        filter.setPage(0);
        filter.setSize(1000);
        var data = productAnalyticsService.getTopProducts(filter);
        String header = "productId;nom;sku;categorie;quantite;ca;marge;remise;stock;rotation\n";
        String rows = data.getItems().stream()
                .map(this::productRow)
                .collect(Collectors.joining("\n"));
        return header + rows;
    }

    private String productRow(AnalyticsProductRow r) {
        return String.join(";",
                str(r.getProductId()),
                esc(r.getProductName()),
                esc(r.getSku()),
                esc(r.getCategoryName()),
                str(r.getQuantitySold()),
                str(r.getRevenue()),
                str(r.getEstimatedMargin()),
                str(r.getDiscountAmount()),
                str(r.getStockRemaining()),
                str(r.getRotationRate()));
    }

    private String exportPayments(AnalyticsFilterRequest filter) {
        var data = paymentAnalyticsService.getPayments(filter);
        String header = "methode;total;transactions;part\n";
        String rows = data.getMethods().stream()
                .map(m -> String.join(";",
                        esc(m.getMethodLabel()),
                        str(m.getTotal()),
                        str(m.getTransactionCount()),
                        str(m.getSharePercent())))
                .collect(Collectors.joining("\n"));
        return header + rows;
    }

    private String exportCashiers(AnalyticsFilterRequest filter) {
        var data = cashierAnalyticsService.getCashiers(filter);
        String header = "caissier;ventes;ca;panierMoyen;remises;remboursements;annulations\n";
        String rows = data.getItems().stream()
                .map(c -> String.join(";",
                        esc(c.getCashierName()),
                        str(c.getSaleCount()),
                        str(c.getRevenue()),
                        str(c.getAverageBasket()),
                        str(c.getDiscountsGranted()),
                        str(c.getRefundsTotal()),
                        str(c.getCancellations())))
                .collect(Collectors.joining("\n"));
        return header + rows;
    }

    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(";", ",").replace("\n", " ");
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
    }
}
