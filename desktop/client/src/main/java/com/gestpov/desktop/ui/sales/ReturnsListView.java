package com.gestpov.desktop.ui.sales;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.SaleRefundBrowsePage;
import com.gestpov.desktop.model.SaleRefundDetail;
import com.gestpov.desktop.model.SaleRefundSummary;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.SalesBrowseClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.ui.products.ProductLabels;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.nio.file.Files;

public final class ReturnsListView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final SalesBrowseClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<SaleRefundSummary> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<String> status = new ComboBox<>();
    private final Label pageInfo = new Label();
    private final Label detailTitle = new Label("Sélectionnez un retour");
    private final Label detailBody = new Label();
    private final ListView<String> lines = new ListView<>();
    private final ListView<String> payments = new ListView<>();
    private int page;

    public ReturnsListView(SessionContext session) {
        this.session = session;
        this.client = new SalesBrowseClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Retours");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Historique des retours et remboursements");
        sub.getStyleClass().add("page-sub");
        pageInfo.getStyleClass().add("page-sub");

        search.setPromptText("N° retour, N° vente, client…");
        search.setOnAction(e -> {
            page = 0;
            reload();
        });
        status.getItems().addAll("", "PENDING", "APPROVED", "COMPLETED", "REJECTED");
        status.setPromptText("Statut");
        status.setPrefWidth(140);

        Button searchBtn = new Button("Rechercher");
        searchBtn.getStyleClass().add("button-secondary");
        searchBtn.setOnAction(e -> {
            page = 0;
            reload();
        });
        Button prev = new Button("←");
        prev.getStyleClass().add("button-ghost");
        prev.setOnAction(e -> {
            if (page > 0) {
                page--;
                reload();
            }
        });
        Button next = new Button("→");
        next.getStyleClass().add("button-ghost");
        next.setOnAction(e -> {
            page++;
            reload();
        });
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());
        Button export = new Button("Export CSV");
        export.getStyleClass().add("button-secondary");
        export.setDisable(!(session.hasPermission("export.read")
                || session.hasPermission("analytics.export")
                || session.hasPermission("pos.report.read")
                || session.hasPermission("pos.return.read")));
        export.setOnAction(e -> exportCsv());

        HBox bar = new HBox(8, search, status, searchBtn, prev, next, pageInfo, refresh, export);
        bar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);

        table.setPlaceholder(new EmptyState("Aucun retour"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("N°", SaleRefundSummary::refundNumber),
                col("Vente", s -> s.saleNumber() == null ? "" : s.saleNumber()),
                col("Statut", SaleRefundSummary::status),
                col("Client", s -> s.customerName() == null ? "—" : s.customerName()),
                col("Montant", s -> ProductLabels.price(s.totalAmount())),
                col("Motif", s -> s.reason() == null ? "—" : s.reason()),
                col("Par", s -> s.createdBy() == null ? "—" : s.createdBy()),
                col("Date", s -> s.createdAt() == null ? "" : s.createdAt())
        );
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null && b.id() != null) {
                loadDetail(b.id());
            }
        });
        HBox.setHgrow(table, Priority.ALWAYS);

        detailTitle.getStyleClass().add("settings-group-title");
        detailBody.getStyleClass().add("page-sub");
        detailBody.setWrapText(true);
        lines.setPlaceholder(new EmptyState("Lignes"));
        payments.setPlaceholder(new EmptyState("Paiements"));
        lines.setPrefHeight(140);
        payments.setPrefHeight(100);
        VBox detail = new VBox(8, detailTitle, detailBody, new Label("Lignes"), lines, new Label("Paiements"), payments);
        detail.getStyleClass().add("card");
        detail.setPadding(new Insets(12));
        detail.setPrefWidth(340);
        detail.setMinWidth(280);

        HBox split = new HBox(16, table, detail);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox pageBox = new VBox(16, title, sub, error, bar, split);
        pageBox.getStyleClass().add("content");
        pageBox.setPadding(new Insets(0));
        return pageBox;
    }

    private void loadDetail(long id) {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> client.returnDetail(id), this::showDetail, this::fail);
    }

    private void showDetail(SaleRefundDetail detail) {
        loading.setLoading(false);
        if (detail == null) {
            detailTitle.setText("Retour introuvable");
            return;
        }
        detailTitle.setText(detail.refundNumber() == null ? "Retour #" + detail.id() : detail.refundNumber());
        detailBody.setText("Statut : " + nullSafe(detail.status())
                + "\nVente : " + nullSafe(detail.saleNumber())
                + "\nClient : " + nullSafe(detail.customerName())
                + "\nMontant : " + ProductLabels.price(detail.totalAmount())
                + "\nMotif : " + nullSafe(detail.reason())
                + (detail.notes() == null || detail.notes().isBlank() ? "" : "\nNotes : " + detail.notes())
                + "\nPar : " + nullSafe(detail.createdBy())
                + "\nDate : " + nullSafe(detail.createdAt()));
        lines.setItems(FXCollections.observableArrayList(
                detail.lines() == null ? java.util.List.of() : detail.lines()));
        payments.setItems(FXCollections.observableArrayList(
                detail.payments() == null ? java.util.List.of() : detail.payments()));
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        String q = search.getText();
        String st = status.getValue();
        int p = page;
        FxAsync.run(() -> client.browseReturns(q, st, p, 50), this::applyPage, this::fail);
    }

    private void applyPage(SaleRefundBrowsePage browse) {
        loading.setLoading(false);
        table.setItems(FXCollections.observableArrayList(browse.items()));
        pageInfo.setText("Page " + (browse.page() + 1) + " / " + Math.max(browse.totalPages(), 1)
                + " (" + browse.totalElements() + ")");
        if (browse.page() >= browse.totalPages() && browse.totalPages() > 0 && page > 0) {
            page = browse.totalPages() - 1;
        }
    }

    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("retours.csv");
        var file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.exportReturnsCsv(search.getText(), status.getValue()), bytes -> {
            try {
                Files.write(file.toPath(), bytes);
                loading.setLoading(false);
            } catch (Exception ex) {
                fail(ex);
            }
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

    private static TableColumn<SaleRefundSummary, String> col(String title,
                                                        java.util.function.Function<SaleRefundSummary, String> fn) {
        TableColumn<SaleRefundSummary, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(fn.apply(cd.getValue()) == null ? "" : fn.apply(cd.getValue())));
        return c;
    }

    private static String nullSafe(String v) {
        return v == null ? "—" : v;
    }
}
