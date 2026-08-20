package com.gestpov.desktop.ui.stock;

import com.gestpov.desktop.model.StockItem;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.StockClient;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class StockView extends StackPane {

    private final StockClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<StockItem> table = new TableView<>();
    private final TextField search = new TextField();
    private List<StockItem> all = List.of();

    public StockView(SessionContext session) {
        this.client = new StockClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Stock");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Quantités par produit et emplacement (lecture API)");
        sub.getStyleClass().add("page-sub");
        search.setPromptText("Filtrer par produit, entrepôt…");
        search.textProperty().addListener((o, a, b) -> applyFilter());
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());
        HBox bar = new HBox(8, search, refresh);
        HBox.setHgrow(search, Priority.ALWAYS);
        VBox card = new VBox(bar);
        card.getStyleClass().add("card");
        table.setPlaceholder(new EmptyState("Aucun stock"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(FXCollections.observableArrayList());
        table.getColumns().addAll(
                col("Produit", s -> s.productNom() == null ? "—" : s.productNom()),
                col("Entrepôt", s -> s.warehouseCode() == null ? "—" : s.warehouseCode()),
                col("Emplacement", s -> s.locationCode() == null ? "—" : s.locationCode()),
                col("En stock", s -> s.quantityOnHand() == null ? "0" : s.quantityOnHand().stripTrailingZeros().toPlainString()),
                col("Disponible", s -> s.quantityAvailable() == null ? "0" : s.quantityAvailable().stripTrailingZeros().toPlainString()),
                col("Unité", s -> s.unitSymbole() == null ? "" : s.unitSymbole())
        );
        VBox page = new VBox(16, title, sub, error, card, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    public void reload() {
        loading.setLoading(true);
        FxAsync.run(client::listItems, list -> {
            loading.setLoading(false);
            all = list;
            applyFilter();
        }, t -> {
            loading.setLoading(false);
            if (t instanceof ApiException api && !api.isUnauthorized()) {
                error.show(ApiException.userMessage(api));
            } else if (!(t instanceof ApiException a && a.isUnauthorized())) {
                error.show("Une erreur est survenue.");
            }
        });
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            table.getItems().setAll(all);
            return;
        }
        table.getItems().setAll(all.stream().filter(s ->
                (s.productNom() != null && s.productNom().toLowerCase(Locale.ROOT).contains(q))
                        || (s.warehouseCode() != null && s.warehouseCode().toLowerCase(Locale.ROOT).contains(q))
                        || (s.locationCode() != null && s.locationCode().toLowerCase(Locale.ROOT).contains(q))
        ).collect(Collectors.toList()));
    }

    private static TableColumn<StockItem, String> col(String title, java.util.function.Function<StockItem, String> fn) {
        TableColumn<StockItem, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
