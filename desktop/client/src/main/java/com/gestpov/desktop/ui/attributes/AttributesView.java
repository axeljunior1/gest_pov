package com.gestpov.desktop.ui.attributes;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.CustomAttribute;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.AttributeClient;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class AttributesView extends StackPane implements Reloadable {

    private static final String[] TYPES = {"TEXT", "NUMBER", "BOOLEAN", "DATE", "SELECT"};

    private final SessionContext session;
    private final AttributeClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<CustomAttribute> table = new TableView<>();
    private final TextField code = new TextField();
    private final TextField label = new TextField();
    private final ComboBox<String> type = new ComboBox<>();

    public AttributesView(SessionContext session) {
        this.session = session;
        this.client = new AttributeClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Attributs");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Définitions d'attributs personnalisés produits");
        sub.getStyleClass().add("page-sub");

        code.setPromptText("Code *");
        label.setPromptText("Libellé *");
        type.getItems().setAll(TYPES);
        type.getSelectionModel().selectFirst();
        Button create = new Button("Créer");
        create.getStyleClass().add("button-primary");
        create.setOnAction(e -> create());
        HBox form = new HBox(8, code, label, type, create);
        form.setAlignment(Pos.CENTER_LEFT);
        boolean canCreate = session.hasPermission("products.create");
        form.setVisible(canCreate);
        form.setManaged(canCreate);
        HBox.setHgrow(label, Priority.ALWAYS);

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        VBox card = new VBox(10, form, refresh);
        card.getStyleClass().add("card");

        table.setItems(FXCollections.observableArrayList());
        table.setPlaceholder(new EmptyState("Aucun attribut"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(col("Code", CustomAttribute::code));
        table.getColumns().add(col("Libellé", CustomAttribute::label));
        table.getColumns().add(col("Type", CustomAttribute::type));
        if (session.hasPermission("products.delete")) {
            TableColumn<CustomAttribute, Void> actions = new TableColumn<>();
            actions.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        return;
                    }
                    CustomAttribute attr = getTableRow().getItem();
                    Button del = new Button("Suppr.");
                    del.getStyleClass().add("button-danger");
                    del.setOnAction(e -> delete(attr));
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
        String c = code.getText() == null ? "" : code.getText().trim();
        String l = label.getText() == null ? "" : label.getText().trim();
        if (c.isEmpty() || l.isEmpty()) {
            error.show("Code et libellé obligatoires.");
            return;
        }
        String t = type.getValue() == null ? "TEXT" : type.getValue();
        loading.setLoading(true);
        FxAsync.run(() -> client.create(c, l, t), created -> {
            code.clear();
            label.clear();
            reload();
        }, this::fail);
    }

    private void delete(CustomAttribute attr) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer", "Supprimer cet attribut ?")) {
            return;
        }
        loading.setLoading(true);
        FxAsync.runVoid(() -> client.delete(attr.id()), this::reload, this::fail);
    }

    @Override
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

    private static TableColumn<CustomAttribute, String> col(String title,
                                                            java.util.function.Function<CustomAttribute, String> fn) {
        TableColumn<CustomAttribute, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
