package com.gestpov.desktop.ui.pos;

import com.gestpov.desktop.ui.Reloadable;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.PosClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.ui.products.ProductLabels;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class PosReportsView extends StackPane implements Reloadable {

    private final PosClient pos;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<JsonNode> table = new TableView<>();
    private final VBox detail = new VBox(4);
    private final Label count = new Label();

    public PosReportsView(SessionContext session) {
        this.pos = new PosClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Rapports de caisse");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Sessions fermées — sélectionnez pour le détail");
        sub.getStyleClass().add("page-sub");
        count.getStyleClass().add("page-sub");

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("Session", n -> text(n, "sessionNumber")),
                col("Caissier", n -> text(n, "cashierName")),
                col("Ouverture", n -> text(n, "openedAt")),
                col("Clôture", n -> text(n, "closedAt")),
                col("Écart", n -> money(n, "differenceAmount"))
        );
        table.setPlaceholder(new EmptyState("Aucune session fermée"));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> loadReport(b));

        detail.getStyleClass().add("card");
        detail.setPadding(new Insets(12));
        detail.getChildren().add(new Label("Sélectionnez une session"));

        HBox bar = new HBox(10, refresh);
        VBox page = new VBox(12, title, sub, error, bar, count, table, detail);
        page.getStyleClass().add("content");
        VBox.setVgrow(table, Priority.ALWAYS);
        return page;
    }

    @Override
    public void reload() {
        loading.setLoading(true);
        FxAsync.run(() -> pos.listClosedSessions(100), list -> {
            loading.setLoading(false);
            table.getItems().setAll(list);
            count.setText(list.size() + " session(s)");
        }, this::fail);
    }

    private void loadReport(JsonNode sessionNode) {
        if (sessionNode == null || !sessionNode.hasNonNull("id")) {
            return;
        }
        long id = sessionNode.get("id").asLong();
        loading.setLoading(true);
        FxAsync.run(() -> pos.sessionReport(id), report -> {
            loading.setLoading(false);
            List<Label> rows = new ArrayList<>();
            rows.add(row("Session", text(report, "sessionNumber")));
            rows.add(row("Caissier", text(report, "cashierName")));
            rows.add(row("Ventes", String.valueOf(report.path("saleCount").asInt(0))));
            rows.add(row("Total", money(report, "totalRevenue")));
            rows.add(row("Espèces", money(report, "cashRevenue")));
            rows.add(row("Carte", money(report, "cardRevenue")));
            rows.add(row("Fond", money(report, "openingCashAmount")));
            rows.add(row("Attendu", money(report, "expectedCashAmount")));
            rows.add(row("Déclaré", money(report, "declaredCashAmount")));
            rows.add(row("Écart", money(report, "cashDifference")));
            if (report.hasNonNull("differenceReasonLabel")) {
                rows.add(row("Motif", report.get("differenceReasonLabel").asText()));
            }
            detail.getChildren().setAll(rows);
        }, this::fail);
    }

    private void fail(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api && !api.isUnauthorized()) {
            error.show(ApiException.userMessage(api));
        } else if (!(t instanceof ApiException a && a.isUnauthorized())) {
            error.show("Une erreur est survenue.");
        }
    }

    private static Label row(String k, String v) {
        Label l = new Label(k + " : " + v);
        l.getStyleClass().add("page-sub");
        return l;
    }

    private static String text(JsonNode n, String f) {
        return n != null && n.hasNonNull(f) ? n.get(f).asText("") : "—";
    }

    private static String money(JsonNode n, String f) {
        if (n == null || !n.hasNonNull(f)) {
            return ProductLabels.price(BigDecimal.ZERO);
        }
        try {
            return ProductLabels.price(new BigDecimal(n.get(f).asText()));
        } catch (Exception e) {
            return n.get(f).asText();
        }
    }

    private static TableColumn<JsonNode, String> col(String title, java.util.function.Function<JsonNode, String> fn) {
        TableColumn<JsonNode, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return c;
    }
}
