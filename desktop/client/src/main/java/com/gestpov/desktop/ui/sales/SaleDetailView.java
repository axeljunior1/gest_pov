package com.gestpov.desktop.ui.sales;

import com.gestpov.desktop.model.SaleDetail;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.SalesBrowseClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Détail d'une vente BO ({@code GET /api/sales/{id}}).
 */
public final class SaleDetailView extends StackPane {

    private final SalesBrowseClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final Label title = new Label("Détail vente");
    private final Label body = new Label();
    private final ListView<String> lines = new ListView<>();
    private final ListView<String> timeline = new ListView<>();
    private final Runnable onBack;
    private final long saleId;

    public SaleDetailView(SessionContext session, long saleId, Runnable onBack) {
        this.client = new SalesBrowseClient(session.api());
        this.saleId = saleId;
        this.onBack = onBack;
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        title.getStyleClass().add("page-title");
        body.getStyleClass().add("page-sub");
        body.setWrapText(true);
        lines.setPlaceholder(new EmptyState("Aucune ligne"));
        timeline.setPlaceholder(new EmptyState("Aucun événement"));
        Button back = new Button("← Retour liste");
        back.getStyleClass().add("button-ghost");
        if (onBack != null) {
            back.setOnAction(e -> onBack.run());
        } else {
            back.setDisable(true);
        }
        VBox.setVgrow(lines, Priority.ALWAYS);
        VBox page = new VBox(12, back, title, error, body, new Label("Lignes"), lines,
                new Label("Timeline"), timeline);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void reload() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> client.detail(saleId), detail -> {
            loading.setLoading(false);
            apply(detail);
        }, t -> {
            loading.setLoading(false);
            if (t instanceof ApiException api) {
                error.show(ApiException.userMessage(api));
            } else {
                error.show(t.getMessage() == null ? "Erreur" : t.getMessage());
            }
        });
    }

    private void apply(SaleDetail detail) {
        if (detail == null || detail.sale() == null) {
            title.setText("Vente introuvable");
            return;
        }
        var sale = detail.sale();
        title.setText(sale.saleNumber() == null ? "Vente #" + sale.id() : sale.saleNumber());
        body.setText("Statut : " + nullSafe(sale.status())
                + "\nClient : " + nullSafe(sale.customerName())
                + "\nVendeur : " + nullSafe(sale.sellerName())
                + "\nTotal : " + (sale.total() == null ? "—" : sale.total())
                + "\nRemboursé : " + (detail.totalRefunded() == null ? "0" : detail.totalRefunded()));
        lines.setItems(FXCollections.observableArrayList(
                sale.lignes() == null ? java.util.List.of()
                        : sale.lignes().stream()
                        .map(l -> (l.productNom() == null ? "?" : l.productNom())
                                + " × " + (l.quantityInput() == null ? "?" : l.quantityInput())
                                + " = " + (l.lineTotal() == null ? "?" : l.lineTotal()))
                        .toList()));
        timeline.setItems(FXCollections.observableArrayList(
                detail.timeline() == null ? java.util.List.of() : detail.timeline()));
    }

    private static String nullSafe(String v) {
        return v == null ? "—" : v;
    }
}
