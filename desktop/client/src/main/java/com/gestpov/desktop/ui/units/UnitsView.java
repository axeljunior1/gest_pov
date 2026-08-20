package com.gestpov.desktop.ui.units;

import com.gestpov.desktop.model.Unit;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.UnitClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ConfirmationDialog;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
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

public final class UnitsView extends StackPane {

    private final SessionContext session;
    private final UnitClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<Unit> table = new TableView<>();
    private final TextField nom = new TextField();
    private final TextField symbole = new TextField();

    public UnitsView(SessionContext session) {
        this.session = session;
        this.client = new UnitClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Unités");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Unités de base du stock");
        sub.getStyleClass().add("page-sub");
        nom.setPromptText("Nom *");
        symbole.setPromptText("Symbole *");
        Button create = new Button("Créer");
        create.getStyleClass().add("button-primary");
        create.setOnAction(e -> create());
        HBox form = new HBox(8, nom, symbole, create);
        form.setAlignment(Pos.CENTER_LEFT);
        boolean canCreate = session.hasPermission("products.create");
        form.setVisible(canCreate);
        form.setManaged(canCreate);
        HBox.setHgrow(nom, Priority.ALWAYS);
        VBox card = new VBox(form);
        card.getStyleClass().add("card");
        table.setItems(FXCollections.observableArrayList());
        table.setPlaceholder(new EmptyState("Aucune unité"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(col("Nom", Unit::nom));
        table.getColumns().add(col("Symbole", Unit::symbole));
        if (session.hasPermission("products.delete")) {
            TableColumn<Unit, Void> actions = new TableColumn<>();
            actions.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        return;
                    }
                    Unit unit = getTableRow().getItem();
                    Button del = new Button("Suppr.");
                    del.getStyleClass().add("button-danger");
                    del.setOnAction(e -> delete(unit));
                    setGraphic(del);
                }
            });
            table.getColumns().add(actions);
        }
        VBox page = new VBox(16, title, sub, error, card, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void create() {
        if (blank(nom) || blank(symbole)) {
            error.show("Le nom et le symbole sont obligatoires.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.create(nom.getText().trim(), symbole.getText().trim()), created -> {
            nom.clear();
            symbole.clear();
            reload();
        }, this::fail);
    }

    private void delete(Unit unit) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer", "Supprimer cette unité ?")) {
            return;
        }
        loading.setLoading(true);
        FxAsync.runVoid(() -> client.delete(unit.id()), this::reload, this::fail);
    }

    public void reload() {
        loading.setLoading(true);
        FxAsync.run(client::findAll, list -> {
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

    private static boolean blank(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private static TableColumn<Unit, String> col(String title, java.util.function.Function<Unit, String> fn) {
        TableColumn<Unit, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
