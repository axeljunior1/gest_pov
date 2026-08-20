package com.gestpov.desktop.ui.stock;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.InventoryCount;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.StockClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
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

/**
 * Liste des inventaires (lecture).
 */
public final class InventoriesView extends StackPane implements Reloadable {

    private final StockClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<InventoryCount> table = new TableView<>();

    public InventoriesView(SessionContext session) {
        this.client = new StockClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Inventaires");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Comptages d'inventaire");
        sub.getStyleClass().add("page-sub");

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        table.setPlaceholder(new EmptyState("Aucun inventaire"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("N°", i -> nz(i.inventoryNumber())),
                col("Référence", i -> nz(i.reference())),
                col("Entrepôt", i -> nz(i.warehouseCode())),
                col("Emplacement", i -> nz(i.locationCode())),
                col("Statut", i -> nz(i.status())),
                col("Créé", i -> nz(i.createdAt()))
        );

        HBox bar = new HBox(refresh);
        VBox page = new VBox(12, title, sub, error, bar, table);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        VBox.setVgrow(table, Priority.ALWAYS);
        return page;
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(client::listInventories, list -> {
            loading.setLoading(false);
            table.getItems().setAll(list);
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

    private static TableColumn<InventoryCount, String> col(String title,
                                                          java.util.function.Function<InventoryCount, String> fn) {
        TableColumn<InventoryCount, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
