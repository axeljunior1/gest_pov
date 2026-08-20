package com.gestpov.desktop.ui.stock;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.StockLocation;
import com.gestpov.desktop.model.StockTransfer;
import com.gestpov.desktop.model.Warehouse;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.ProductClient;
import com.gestpov.desktop.net.StockClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.List;

/**
 * Transferts inter-entrepôts — liste, création simple, expédier / réceptionner.
 */
public final class StockTransfersView extends StackPane implements Reloadable {

    private final StockClient client;
    private final ProductClient products;
    private final boolean canAdjust;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<StockTransfer> table = new TableView<>();

    private final TextField referenceField = new TextField();
    private final ComboBox<Warehouse> sourceWh = new ComboBox<>();
    private final ComboBox<Warehouse> destWh = new ComboBox<>();
    private final ComboBox<StockLocation> sourceLoc = new ComboBox<>();
    private final ComboBox<StockLocation> destLoc = new ComboBox<>();
    private final ComboBox<ProductOption> productCombo = new ComboBox<>();
    private final TextField qtyField = new TextField();

    public StockTransfersView(SessionContext session) {
        this.client = new StockClient(session.api());
        this.products = new ProductClient(session.api());
        this.canAdjust = session.hasPermission("stock.adjust");
        getChildren().addAll(build(), loading);
        reload();
        if (canAdjust) {
            reloadRefs();
        }
    }

    private VBox build() {
        Label title = new Label("Transferts");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Transferts entre entrepôts");
        sub.getStyleClass().add("page-sub");

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        Button ship = new Button("Expédier");
        ship.getStyleClass().add("button-secondary");
        ship.setDisable(!canAdjust);
        ship.setOnAction(e -> shipSelected());
        Button receive = new Button("Réceptionner");
        receive.getStyleClass().add("button-primary");
        receive.setDisable(!canAdjust);
        receive.setOnAction(e -> receiveSelected());

        table.setPlaceholder(new EmptyState("Aucun transfert"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("Réf.", t -> nz(t.reference())),
                col("Source", t -> nz(t.sourceWarehouseCode())),
                col("Dest.", t -> nz(t.destWarehouseCode())),
                col("Statut", t -> nz(t.status())),
                col("Créé", t -> nz(t.createdAt()))
        );

        HBox bar = new HBox(8, refresh, ship, receive);
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(12, title, sub, error, bar, table);
        if (canAdjust) {
            page.getChildren().add(buildCreateForm());
        }
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        VBox.setVgrow(table, Priority.ALWAYS);
        return page;
    }

    private VBox buildCreateForm() {
        referenceField.setPromptText("Référence");
        sourceWh.setPromptText("Entrepôt source");
        destWh.setPromptText("Entrepôt destination");
        sourceLoc.setPromptText("Emplacement source");
        destLoc.setPromptText("Emplacement destination");
        productCombo.setPromptText("Produit");
        qtyField.setPromptText("Quantité");
        sourceWh.setMaxWidth(Double.MAX_VALUE);
        destWh.setMaxWidth(Double.MAX_VALUE);
        sourceLoc.setMaxWidth(Double.MAX_VALUE);
        destLoc.setMaxWidth(Double.MAX_VALUE);
        productCombo.setMaxWidth(Double.MAX_VALUE);
        sourceWh.valueProperty().addListener((o, a, b) -> loadLocs(b, sourceLoc));
        destWh.valueProperty().addListener((o, a, b) -> loadLocs(b, destLoc));

        Button create = new Button("Créer transfert");
        create.getStyleClass().add("button-primary");
        create.setOnAction(e -> createTransfer());

        VBox form = new VBox(8,
                new HBox(8, referenceField, productCombo, qtyField),
                new HBox(8, sourceWh, sourceLoc),
                new HBox(8, destWh, destLoc),
                create
        );
        form.getStyleClass().add("card");
        Label h = new Label("Nouveau transfert (1 ligne)");
        h.getStyleClass().add("settings-group-title");
        return new VBox(8, h, form);
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(client::listTransfers, list -> {
            loading.setLoading(false);
            table.getItems().setAll(list);
        }, this::fail);
    }

    private void reloadRefs() {
        FxAsync.run(() -> {
            List<Warehouse> wh = client.listWarehouses();
            List<Product> prods = products.search(new com.gestpov.desktop.model.ProductQuery());
            return new Refs(wh, prods);
        }, refs -> {
            sourceWh.getItems().setAll(refs.warehouses());
            destWh.getItems().setAll(refs.warehouses());
            productCombo.getItems().setAll(refs.products().stream()
                    .map(p -> new ProductOption(p.id(), p.nom(), p.sku()))
                    .toList());
        }, ignored -> {
        });
    }

    private void loadLocs(Warehouse warehouse, ComboBox<StockLocation> target) {
        target.getItems().clear();
        if (warehouse == null || warehouse.id() == null) {
            return;
        }
        FxAsync.run(() -> client.listLocations(warehouse.id()), locs -> {
            target.getItems().setAll(locs);
            if (!locs.isEmpty()) {
                target.getSelectionModel().selectFirst();
            }
        }, this::fail);
    }

    private void createTransfer() {
        error.hide();
        Warehouse src = sourceWh.getValue();
        Warehouse dst = destWh.getValue();
        StockLocation srcLoc = sourceLoc.getValue();
        StockLocation dstLoc = destLoc.getValue();
        ProductOption product = productCombo.getValue();
        String ref = referenceField.getText() == null ? "" : referenceField.getText().trim();
        if (ref.isEmpty() || src == null || dst == null || srcLoc == null || dstLoc == null || product == null
                || product.id() == null || src.id() == null || dst.id() == null
                || srcLoc.id() == null || dstLoc.id() == null) {
            error.show("Renseignez référence, entrepôts, emplacements et produit.");
            return;
        }
        if (src.id().equals(dst.id())) {
            error.show("Source et destination doivent différer.");
            return;
        }
        BigDecimal qty;
        try {
            qty = new BigDecimal(qtyField.getText().trim().replace(',', '.'));
        } catch (Exception e) {
            error.show("Quantité invalide.");
            return;
        }
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            error.show("Quantité positive requise.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.createTransfer(ref, src.id(), dst.id(), product.id(), qty,
                srcLoc.id(), dstLoc.id(), null), created -> {
            loading.setLoading(false);
            referenceField.clear();
            qtyField.clear();
            reload();
        }, this::fail);
    }

    private void shipSelected() {
        StockTransfer t = table.getSelectionModel().getSelectedItem();
        if (t == null || t.id() == null) {
            error.show("Sélectionnez un transfert.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.shipTransfer(t.id()), updated -> {
            loading.setLoading(false);
            reload();
        }, this::fail);
    }

    private void receiveSelected() {
        StockTransfer t = table.getSelectionModel().getSelectedItem();
        if (t == null || t.id() == null) {
            error.show("Sélectionnez un transfert.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.receiveTransfer(t.id()), updated -> {
            loading.setLoading(false);
            reload();
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

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static TableColumn<StockTransfer, String> col(String title,
                                                         java.util.function.Function<StockTransfer, String> fn) {
        TableColumn<StockTransfer, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private record Refs(List<Warehouse> warehouses, List<Product> products) {
    }

    private record ProductOption(Long id, String nom, String sku) {
        @Override
        public String toString() {
            if (sku == null || sku.isBlank()) {
                return nom == null ? "" : nom;
            }
            return nom + " (" + sku + ")";
        }
    }
}
