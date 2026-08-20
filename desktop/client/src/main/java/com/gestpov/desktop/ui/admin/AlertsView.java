package com.gestpov.desktop.ui.admin;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.Alert;
import com.gestpov.desktop.net.AlertClient;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.ListPager;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class AlertsView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final AlertClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<Alert> table = new TableView<>();
    private final ListPager<Alert> pager = new ListPager<>(table);
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> typeFilter = new ComboBox<>();

    public AlertsView(SessionContext session) {
        this.session = session;
        this.client = new AlertClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Alertes");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Stock bas, ruptures, péremption");
        sub.getStyleClass().add("page-sub");

        statusFilter.getItems().addAll("", "OPEN", "ACKNOWLEDGED", "RESOLVED", "IGNORED");
        statusFilter.setPromptText("Statut");
        statusFilter.setValue("OPEN");
        typeFilter.getItems().addAll("", "LOW_STOCK", "OUT_OF_STOCK", "EXPIRY_SOON", "EXPIRED");
        typeFilter.setPromptText("Type");

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());
        HBox filters = new HBox(8, new Label("Statut"), statusFilter, new Label("Type"), typeFilter, refresh);
        filters.setAlignment(Pos.CENTER_LEFT);

        table.setPlaceholder(new EmptyState("Aucune alerte"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("Type", Alert::type),
                col("Sévérité", Alert::severity),
                col("Statut", Alert::status),
                col("Produit", a -> a.productNom() == null ? "" : a.productNom()),
                col("Entrepôt", a -> a.warehouseCode() == null ? "" : a.warehouseCode()),
                col("Message", a -> a.message() == null ? "" : a.message())
        );
        TableColumn<Alert, Void> actions = new TableColumn<>();
        actions.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Alert alert = getTableRow().getItem();
                if (!session.hasPermission("alerts.manage")) {
                    setGraphic(null);
                    return;
                }
                Button ack = new Button("Ack");
                ack.getStyleClass().add("button-ghost");
                ack.setOnAction(e -> act(() -> client.acknowledge(alert.id())));
                Button resolve = new Button("Résoudre");
                resolve.getStyleClass().add("button-ghost");
                resolve.setOnAction(e -> act(() -> client.resolve(alert.id())));
                Button ignore = new Button("Ignorer");
                ignore.getStyleClass().add("button-ghost");
                ignore.setOnAction(e -> act(() -> client.ignore(alert.id())));
                setGraphic(new HBox(6, ack, resolve, ignore));
            }
        });
        table.getColumns().add(actions);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox page = new VBox(16, title, sub, error, filters, table, pager.bar());
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void act(FxAsync.ThrowingRunnable work) {
        error.hide();
        loading.setLoading(true);
        FxAsync.runVoid(work, this::reload, this::fail);
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        String status = statusFilter.getValue();
        String type = typeFilter.getValue();
        FxAsync.run(() -> client.list(type, status), list -> {
            loading.setLoading(false);
            pager.setItems(list == null ? List.of() : list);
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

    private static TableColumn<Alert, String> col(String title, java.util.function.Function<Alert, String> fn) {
        TableColumn<Alert, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(fn.apply(cd.getValue()) == null ? "" : fn.apply(cd.getValue())));
        return c;
    }
}
