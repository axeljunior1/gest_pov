package com.gestpov.desktop.ui.sales;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.SaleBrowsePage;
import com.gestpov.desktop.model.SaleDetail;
import com.gestpov.desktop.model.SaleSummary;
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

public final class SalesListView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final SalesBrowseClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<SaleSummary> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<String> status = new ComboBox<>();
    private final Label pageInfo = new Label();
    private final Label detailTitle = new Label("Sélectionnez une vente");
    private final Label detailBody = new Label();
    private final ListView<String> lines = new ListView<>();
    private final ListView<String> timeline = new ListView<>();
    private int page;

    public SalesListView(SessionContext session) {
        this.session = session;
        this.client = new SalesBrowseClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Ventes");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Consultation back-office des ventes");
        sub.getStyleClass().add("page-sub");
        pageInfo.getStyleClass().add("page-sub");

        search.setPromptText("N° vente, client…");
        search.setOnAction(e -> {
            page = 0;
            reload();
        });
        status.getItems().addAll("", "DRAFT", "PENDING_PAYMENT", "PAID", "CANCELLED", "VALIDATED");
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
        Button export = new Button("Export CSV");
        export.getStyleClass().add("button-secondary");
        export.setDisable(!(session.hasPermission("export.read")
                || session.hasPermission("analytics.export")
                || session.hasPermission("pos.report.read")
                || session.hasPermission("analytics.sales.read")));
        export.setOnAction(e -> exportCsv());

        HBox bar = new HBox(8, search, status, searchBtn, prev, next, pageInfo, export);
        bar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);

        table.setPlaceholder(new EmptyState("Aucune vente"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("N°", SaleSummary::saleNumber),
                col("Statut", SaleSummary::status),
                col("Client", s -> s.customerName() == null ? "" : s.customerName()),
                col("Vendeur", s -> s.sellerName() == null ? "—" : s.sellerName()),
                col("Caissier", s -> s.cashierName() == null ? "—" : s.cashierName()),
                col("Total", s -> ProductLabels.price(s.total())),
                col("Retours", s -> s.refundCount() > 0
                        ? s.refundCount() + " · " + ProductLabels.price(s.totalRefunded())
                        : "—"),
                col("Date", s -> {
                    String date = s.paidAt() != null ? s.paidAt() : s.createdAt();
                    return date == null ? "" : date;
                })
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
        timeline.setPlaceholder(new EmptyState("Timeline"));
        lines.setPrefHeight(140);
        timeline.setPrefHeight(100);
        VBox detail = new VBox(8, detailTitle, detailBody, new Label("Lignes"), lines, new Label("Timeline"), timeline);
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
        FxAsync.run(() -> client.detail(id), this::showDetail, this::fail);
    }

    private void showDetail(SaleDetail detail) {
        loading.setLoading(false);
        if (detail == null || detail.sale() == null) {
            detailTitle.setText("Vente introuvable");
            return;
        }
        var sale = detail.sale();
        detailTitle.setText(sale.saleNumber() == null ? "Vente #" + sale.id() : sale.saleNumber());
        detailBody.setText("Statut : " + nullSafe(sale.status())
                + "\nClient : " + nullSafe(sale.customerName())
                + "\nVendeur : " + nullSafe(sale.sellerName())
                + "\nTotal : " + ProductLabels.price(sale.total())
                + "\nReçu : " + ProductLabels.price(sale.paidAmount())
                + "\nMonnaie rendue : " + ProductLabels.price(sale.changeAmount())
                + "\nRemboursé : " + ProductLabels.price(detail.totalRefunded()));
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

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        String q = search.getText();
        String st = status.getValue();
        int p = page;
        FxAsync.run(() -> client.browse(q, st, p, 50), this::applyPage, this::fail);
    }

    private void applyPage(SaleBrowsePage browse) {
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
        chooser.setInitialFileName("ventes.csv");
        var file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.exportCsv(search.getText(), status.getValue()), bytes -> {
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

    private static TableColumn<SaleSummary, String> col(String title,
                                                        java.util.function.Function<SaleSummary, String> fn) {
        TableColumn<SaleSummary, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(fn.apply(cd.getValue()) == null ? "" : fn.apply(cd.getValue())));
        return c;
    }

    private static String nullSafe(String v) {
        return v == null ? "—" : v;
    }
}
