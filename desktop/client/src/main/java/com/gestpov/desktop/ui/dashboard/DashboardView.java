package com.gestpov.desktop.ui.dashboard;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.DashboardAlertSummary;
import com.gestpov.desktop.model.DashboardSummary;
import com.gestpov.desktop.model.StockMovement;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.DashboardClient;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public final class DashboardView extends StackPane implements Reloadable {

    private final DashboardClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final Label products = metric("—");
    private final Label qty = metric("—");
    private final Label value = metric("—");
    private final Label out = metric("—");
    private final Label low = metric("—");
    private final Label openAlerts = metric("—");
    private final ListView<String> movements = new ListView<>();
    private final ListView<String> topMoved = new ListView<>();
    private final ListView<String> warehouses = new ListView<>();

    public DashboardView(SessionContext session) {
        this.client = new DashboardClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Tableau de bord");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Synthèse stock et alertes");
        sub.getStyleClass().add("page-sub");

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        GridPane kpis = new GridPane();
        kpis.setHgap(12);
        kpis.setVgap(12);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(33.3);
            kpis.getColumnConstraints().add(c);
        }
        kpis.add(kpiCard("Produits", products), 0, 0);
        kpis.add(kpiCard("Quantité stock", qty), 1, 0);
        kpis.add(kpiCard("Valeur stock", value), 2, 0);
        kpis.add(kpiCard("Ruptures", out), 0, 1);
        kpis.add(kpiCard("Stock bas", low), 1, 1);
        kpis.add(kpiCard("Alertes ouvertes", openAlerts), 2, 1);

        movements.setPlaceholder(new EmptyState("Aucun mouvement"));
        topMoved.setPlaceholder(new EmptyState("—"));
        warehouses.setPlaceholder(new EmptyState("—"));
        movements.setPrefHeight(180);
        topMoved.setPrefHeight(180);
        warehouses.setPrefHeight(180);

        VBox movCard = listCard("Mouvements récents", movements);
        VBox topCard = listCard("Produits les plus mouvementés", topMoved);
        VBox whCard = listCard("Entrepôts", warehouses);
        HBox lists = new HBox(12, movCard, topCard, whCard);
        HBox.setHgrow(movCard, Priority.ALWAYS);
        HBox.setHgrow(topCard, Priority.ALWAYS);
        HBox.setHgrow(whCard, Priority.ALWAYS);
        VBox.setVgrow(lists, Priority.ALWAYS);

        VBox page = new VBox(16, title, sub, error, refresh, kpis, lists);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> {
            DashboardSummary summary = client.summary();
            DashboardAlertSummary alerts = client.alerts();
            List<StockMovement> mov = client.recentMovements(10);
            List<String> top = client.topMoved(8);
            List<String> wh = client.warehouses();
            return new Object[]{summary, alerts, mov, top, wh};
        }, data -> {
            loading.setLoading(false);
            DashboardSummary summary = (DashboardSummary) data[0];
            DashboardAlertSummary alerts = (DashboardAlertSummary) data[1];
            @SuppressWarnings("unchecked")
            List<StockMovement> mov = (List<StockMovement>) data[2];
            @SuppressWarnings("unchecked")
            List<String> top = (List<String>) data[3];
            @SuppressWarnings("unchecked")
            List<String> wh = (List<String>) data[4];
            if (summary != null) {
                products.setText(String.valueOf(summary.totalProducts()));
                qty.setText(summary.totalStockQuantity() == null ? "0" : summary.totalStockQuantity().toPlainString());
                value.setText(summary.stockValue() == null ? "0" : summary.stockValue().toPlainString());
                out.setText(String.valueOf(summary.outOfStockProducts()));
                low.setText(String.valueOf(summary.lowStockProducts()));
            }
            if (alerts != null) {
                openAlerts.setText(String.valueOf(alerts.openAlerts()));
            }
            movements.setItems(FXCollections.observableArrayList(
                    mov.stream().map(m -> m.typeLabel() + " · " + nullSafe(m.productNom())
                            + " · " + m.quantity()).toList()));
            topMoved.setItems(FXCollections.observableArrayList(top));
            warehouses.setItems(FXCollections.observableArrayList(wh));
        }, this::fail);
    }

    private void fail(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api) {
            error.show(ApiException.userMessage(api));
        } else {
            error.show(t.getMessage() == null ? "Erreur" : t.getMessage());
        }
    }

    private static VBox kpiCard(String label, Label value) {
        Label l = new Label(label);
        l.getStyleClass().add("page-sub");
        VBox box = new VBox(6, l, value);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));
        return box;
    }

    private static VBox listCard(String title, ListView<String> list) {
        Label h = new Label(title);
        h.getStyleClass().add("settings-group-title");
        VBox box = new VBox(8, h, list);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private static Label metric(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-title");
        return l;
    }

    private static String nullSafe(String v) {
        return v == null ? "" : v;
    }
}
