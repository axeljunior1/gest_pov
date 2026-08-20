package com.gestpov.desktop.ui.pos;

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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;

public final class PosReturnsView extends StackPane {

    private final PosClient pos;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TextField search = new TextField();
    private final TableView<JsonNode> sales = new TableView<>();
    private final Label detail = new Label();
    private final ComboBox<String> payMethod = new ComboBox<>();
    private final TextField reason = new TextField();
    private JsonNode selectedReturnable;

    public PosReturnsView(SessionContext session) {
        this.pos = new PosClient(session.api());
        getChildren().addAll(build(), loading);
    }

    private VBox build() {
        Label title = new Label("Retours POS");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Rechercher une vente remboursable, créer puis valider le retour");
        sub.getStyleClass().add("page-sub");

        search.setPromptText("N° vente ou client…");
        Button find = new Button("Chercher");
        find.getStyleClass().add("button-secondary");
        find.setOnAction(e -> searchSales());

        sales.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        sales.getColumns().addAll(
                col("N°", n -> text(n, "saleNumber")),
                col("Client", n -> text(n, "customerName")),
                col("Total", n -> money(n, "total")),
                col("Remboursable", n -> money(n, "amountRefundable"))
        );
        sales.setPlaceholder(new EmptyState("Recherchez une vente"));
        sales.setPrefHeight(220);
        sales.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> loadReturnable(b));

        detail.getStyleClass().add("page-sub");
        detail.setWrapText(true);
        reason.setPromptText("Motif du retour");
        payMethod.getItems().addAll("CASH", "CARD", "MOBILE_MONEY");
        payMethod.getSelectionModel().select("CASH");

        Button create = new Button("Créer retour complet");
        create.getStyleClass().add("button-primary");
        create.setOnAction(e -> createAndValidate());

        HBox bar = new HBox(10, search, find);
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox actions = new HBox(10, reason, payMethod, create);
        HBox.setHgrow(reason, Priority.ALWAYS);

        VBox page = new VBox(12, title, sub, error, bar, sales, detail, actions);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void searchSales() {
        String q = search.getText() == null ? "" : search.getText().trim();
        if (q.isEmpty()) {
            error.show("Saisissez un critère de recherche.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.searchRefundable(q, 50), list -> {
            loading.setLoading(false);
            sales.getItems().setAll(list);
            detail.setText(list.size() + " résultat(s)");
        }, this::fail);
    }

    private void loadReturnable(JsonNode saleNode) {
        selectedReturnable = null;
        if (saleNode == null || !saleNode.hasNonNull("id")) {
            return;
        }
        long id = saleNode.get("id").asLong();
        loading.setLoading(true);
        FxAsync.run(() -> pos.returnableSale(id), node -> {
            loading.setLoading(false);
            selectedReturnable = node;
            detail.setText("Vente " + text(node, "saleNumber")
                    + " — remboursable " + money(node, "amountRefundable")
                    + " (déjà remboursé " + money(node, "amountAlreadyRefunded") + ")");
        }, this::fail);
    }

    private void createAndValidate() {
        if (selectedReturnable == null || !selectedReturnable.hasNonNull("id")) {
            error.show("Sélectionnez une vente remboursable.");
            return;
        }
        long saleId = selectedReturnable.get("id").asLong();
        BigDecimal amount;
        try {
            amount = new BigDecimal(selectedReturnable.path("amountRefundable").asText("0"));
        } catch (Exception e) {
            error.show("Montant remboursable invalide.");
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            error.show("Rien à rembourser sur cette vente.");
            return;
        }
        String motif = reason.getText();
        String method = payMethod.getValue() == null ? "CASH" : payMethod.getValue();
        loading.setLoading(true);
        FxAsync.run(() -> {
            JsonNode created = pos.createReturn(saleId, motif, true);
            long returnId = created.get("id").asLong();
            BigDecimal total = created.hasNonNull("totalAmount")
                    ? new BigDecimal(created.get("totalAmount").asText())
                    : amount;
            return pos.validateReturn(returnId, method, total);
        }, validated -> {
            loading.setLoading(false);
            error.hide();
            javafx.scene.control.Alert done = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Retour " + text(validated, "refundNumber") + " validé — "
                            + money(validated, "totalAmount"));
            done.setHeaderText("Retour");
            done.showAndWait();
            selectedReturnable = null;
            searchSales();
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
