package com.gestpov.desktop.ui.stock;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.StockLocation;
import com.gestpov.desktop.model.Warehouse;
import com.gestpov.desktop.net.ApiException;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Entrepôts + emplacements — liste et création (stock.adjust).
 */
public final class WarehousesView extends StackPane implements Reloadable {

    private final StockClient client;
    private final boolean canAdjust;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();

    private final TableView<Warehouse> warehouseTable = new TableView<>();
    private final TableView<StockLocation> locationTable = new TableView<>();
    private final TextField whCode = new TextField();
    private final TextField whNom = new TextField();
    private final TextField whAdresse = new TextField();
    private final TextField locCode = new TextField();
    private final TextField locNom = new TextField();

    public WarehousesView(SessionContext session) {
        this.client = new StockClient(session.api());
        this.canAdjust = session.hasPermission("stock.adjust");
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Entrepôts");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Entrepôts et emplacements");
        sub.getStyleClass().add("page-sub");

        warehouseTable.setPlaceholder(new EmptyState("Aucun entrepôt"));
        warehouseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        warehouseTable.getColumns().addAll(
                colWh("Code", w -> w.code() == null ? "—" : w.code()),
                colWh("Nom", w -> w.nom() == null ? "—" : w.nom())
        );
        warehouseTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> loadLocations(b));

        locationTable.setPlaceholder(new EmptyState("Sélectionnez un entrepôt"));
        locationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        locationTable.getColumns().addAll(
                colLoc("Code", l -> l.code() == null ? "—" : l.code()),
                colLoc("Nom", l -> l.nom() == null ? "—" : l.nom())
        );

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        VBox left = new VBox(8, new Label("Entrepôts"), warehouseTable);
        VBox.setVgrow(warehouseTable, Priority.ALWAYS);
        VBox right = new VBox(8, new Label("Emplacements"), locationTable);
        VBox.setVgrow(locationTable, Priority.ALWAYS);
        HBox tables = new HBox(12, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        VBox.setVgrow(tables, Priority.ALWAYS);

        VBox page = new VBox(12, title, sub, error, refresh, tables);
        if (canAdjust) {
            page.getChildren().add(buildCreateForms());
        }
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private VBox buildCreateForms() {
        whCode.setPromptText("Code");
        whNom.setPromptText("Nom");
        whAdresse.setPromptText("Adresse (optionnel)");
        Button createWh = new Button("Créer entrepôt");
        createWh.getStyleClass().add("button-primary");
        createWh.setOnAction(e -> createWarehouse());
        HBox whRow = new HBox(8, whCode, whNom, whAdresse, createWh);
        whRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(whAdresse, Priority.ALWAYS);

        locCode.setPromptText("Code emplacement");
        locNom.setPromptText("Nom emplacement");
        Button createLoc = new Button("Créer emplacement");
        createLoc.getStyleClass().add("button-primary");
        createLoc.setOnAction(e -> createLocation());
        HBox locRow = new HBox(8, locCode, locNom, createLoc);
        locRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10,
                labeled("Nouvel entrepôt", whRow),
                labeled("Nouvel emplacement (entrepôt sélectionné)", locRow));
        card.getStyleClass().add("card");
        return card;
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(client::listWarehouses, list -> {
            loading.setLoading(false);
            warehouseTable.getItems().setAll(list);
            if (!list.isEmpty() && warehouseTable.getSelectionModel().getSelectedItem() == null) {
                warehouseTable.getSelectionModel().selectFirst();
            }
        }, this::fail);
    }

    private void loadLocations(Warehouse warehouse) {
        locationTable.getItems().clear();
        if (warehouse == null || warehouse.id() == null) {
            return;
        }
        FxAsync.run(() -> client.listLocations(warehouse.id()), locs -> {
            locationTable.getItems().setAll(locs);
        }, this::fail);
    }

    private void createWarehouse() {
        error.hide();
        String code = whCode.getText() == null ? "" : whCode.getText().trim();
        String nom = whNom.getText() == null ? "" : whNom.getText().trim();
        if (code.isEmpty() || nom.isEmpty()) {
            error.show("Code et nom requis.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.createWarehouse(code, nom, whAdresse.getText()), created -> {
            loading.setLoading(false);
            whCode.clear();
            whNom.clear();
            whAdresse.clear();
            reload();
        }, this::fail);
    }

    private void createLocation() {
        error.hide();
        Warehouse wh = warehouseTable.getSelectionModel().getSelectedItem();
        if (wh == null || wh.id() == null) {
            error.show("Sélectionnez un entrepôt.");
            return;
        }
        String code = locCode.getText() == null ? "" : locCode.getText().trim();
        String nom = locNom.getText() == null ? "" : locNom.getText().trim();
        if (code.isEmpty() || nom.isEmpty()) {
            error.show("Code et nom d'emplacement requis.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.createLocation(wh.id(), code, nom), created -> {
            loading.setLoading(false);
            locCode.clear();
            locNom.clear();
            loadLocations(wh);
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

    private static VBox labeled(String title, javafx.scene.Node node) {
        Label l = new Label(title);
        l.getStyleClass().add("form-label");
        return new VBox(4, l, node);
    }

    private static TableColumn<Warehouse, String> colWh(String title, java.util.function.Function<Warehouse, String> fn) {
        TableColumn<Warehouse, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static TableColumn<StockLocation, String> colLoc(String title,
                                                            java.util.function.Function<StockLocation, String> fn) {
        TableColumn<StockLocation, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
