package com.gestpov.desktop.ui.analytics;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.AnalyticsOverview;
import com.gestpov.desktop.model.CancelledSale;
import com.gestpov.desktop.net.AnalyticsClient;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class AnalyticsView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final AnalyticsClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final Label revenueToday = kpi("—");
    private final Label revenueWeek = kpi("—");
    private final Label revenueMonth = kpi("—");
    private final Label salesToday = kpi("—");
    private final Label basket = kpi("—");
    private final Label refunds = kpi("—");
    private final Label cancelledAmt = kpi("—");
    private final Label period = new Label();
    private final TableView<CancelledSale> cancelledTable = new TableView<>();

    public AnalyticsView(SessionContext session) {
        this.session = session;
        this.client = new AnalyticsClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Analytics");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Vue d'ensemble ventes et annulations");
        sub.getStyleClass().add("page-sub");
        period.getStyleClass().add("page-sub");

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        GridPane kpis = new GridPane();
        kpis.setHgap(12);
        kpis.setVgap(12);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(25);
            kpis.getColumnConstraints().add(c);
        }
        kpis.add(card("CA jour", revenueToday), 0, 0);
        kpis.add(card("CA semaine", revenueWeek), 1, 0);
        kpis.add(card("CA mois", revenueMonth), 2, 0);
        kpis.add(card("Ventes jour", salesToday), 3, 0);
        kpis.add(card("Panier moyen", basket), 0, 1);
        kpis.add(card("Remboursements", refunds), 1, 1);
        kpis.add(card("Annulations (montant)", cancelledAmt), 2, 1);

        cancelledTable.setPlaceholder(new EmptyState("Aucune vente annulée"));
        cancelledTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cancelledTable.getColumns().addAll(
                col("N°", CancelledSale::saleNumber),
                col("Client", c -> c.customerName() == null ? "" : c.customerName()),
                col("Total", c -> c.total() == null ? "" : c.total().toPlainString()),
                col("Motif", c -> c.cancellationReasonLabel() == null
                        ? (c.cancellationReason() == null ? "" : c.cancellationReason())
                        : c.cancellationReasonLabel()),
                col("Annulée le", c -> c.cancelledAt() == null ? "" : c.cancelledAt())
        );
        VBox.setVgrow(cancelledTable, Priority.ALWAYS);

        Tab overview = new Tab("Vue d'ensemble", new VBox(12, period, kpis));
        overview.setClosable(false);
        Tab cancelled = new Tab("Ventes annulées", cancelledTable);
        cancelled.setClosable(false);
        TabPane tabs = new TabPane(overview, cancelled);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        boolean canCancel = session.hasPermission("sales.cancellations.read")
                || session.hasPermission("analytics.read");
        if (!canCancel) {
            tabs.getTabs().remove(cancelled);
        }

        VBox page = new VBox(16, title, sub, error, refresh, tabs);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        boolean canOverview = session.hasPermission("analytics.read")
                || session.hasPermission("analytics.sales.read");
        boolean canCancel = session.hasPermission("sales.cancellations.read")
                || session.hasPermission("analytics.read");
        FxAsync.run(() -> {
            AnalyticsOverview overview = canOverview ? client.overview() : null;
            var cancelled = canCancel ? client.cancelledSales() : java.util.List.<CancelledSale>of();
            return new Object[]{overview, cancelled};
        }, data -> {
            loading.setLoading(false);
            AnalyticsOverview overview = (AnalyticsOverview) data[0];
            @SuppressWarnings("unchecked")
            var cancelled = (java.util.List<CancelledSale>) data[1];
            if (overview != null) {
                period.setText((overview.periodLabel() == null ? "" : overview.periodLabel())
                        + (overview.currency() == null ? "" : " · " + overview.currency()));
                revenueToday.setText(dec(overview.revenueToday()));
                revenueWeek.setText(dec(overview.revenueWeek()));
                revenueMonth.setText(dec(overview.revenueMonth()));
                salesToday.setText(dec(overview.salesCountToday()));
                basket.setText(dec(overview.averageBasketToday()));
                refunds.setText(dec(overview.refundsTotal()));
                cancelledAmt.setText(dec(overview.cancelledAmountTotal()));
            }
            cancelledTable.setItems(FXCollections.observableArrayList(cancelled));
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

    private static VBox card(String label, Label value) {
        Label l = new Label(label);
        l.getStyleClass().add("page-sub");
        VBox box = new VBox(6, l, value);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));
        return box;
    }

    private static Label kpi(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("page-title");
        return l;
    }

    private static String dec(java.math.BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }

    private static TableColumn<CancelledSale, String> col(String title,
                                                          java.util.function.Function<CancelledSale, String> fn) {
        TableColumn<CancelledSale, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(fn.apply(cd.getValue()) == null ? "" : fn.apply(cd.getValue())));
        return c;
    }
}
