package com.gestpov.desktop.ui.stock;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.StockValuationOverview;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.StockClient;
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

/**
 * Valorisation stock — overview CMP / analytique.
 */
public final class StockValuationView extends StackPane implements Reloadable {

    private final StockClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final Label totalLabel = new Label();
    private final TableView<StockValuationOverview.CategoryRow> table = new TableView<>();

    public StockValuationView(SessionContext session) {
        this.client = new StockClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Valorisation");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Valeur totale du stock et répartition par catégorie");
        sub.getStyleClass().add("page-sub");
        totalLabel.getStyleClass().add("page-sub");

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        table.setPlaceholder(new EmptyState("Aucune répartition"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("Catégorie", r -> r.categoryName() == null ? "—" : r.categoryName()),
                col("Valeur", r -> ProductLabels.price(r.stockValue() == null ? BigDecimal.ZERO : r.stockValue()))
        );

        HBox bar = new HBox(refresh);
        VBox page = new VBox(12, title, sub, error, bar, totalLabel, table);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        VBox.setVgrow(table, Priority.ALWAYS);
        return page;
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(client::getValuationOverview, overview -> {
            loading.setLoading(false);
            BigDecimal total = overview.totalStockValue() == null ? BigDecimal.ZERO : overview.totalStockValue();
            totalLabel.setText("Valeur totale : " + ProductLabels.price(total));
            table.getItems().setAll(overview.byCategory());
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

    private static TableColumn<StockValuationOverview.CategoryRow, String> col(
            String title,
            java.util.function.Function<StockValuationOverview.CategoryRow, String> fn) {
        TableColumn<StockValuationOverview.CategoryRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
