package com.gestpov.desktop.ui.customers;

import com.gestpov.desktop.model.Customer;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.CustomerClient;
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

public final class CustomersView extends StackPane {

    private final SessionContext session;
    private final CustomerClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<Customer> table = new TableView<>();
    private final TextField firstName = new TextField();
    private final TextField lastName = new TextField();
    private final TextField phone = new TextField();
    private final TextField email = new TextField();
    private final TextField search = new TextField();
    private final Button save = new Button("Créer");
    private Long editingId;

    public CustomersView(SessionContext session) {
        this.session = session;
        this.client = new CustomerClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Clients");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Fiches clients et coordonnées");
        sub.getStyleClass().add("page-sub");
        firstName.setPromptText("Prénom *");
        lastName.setPromptText("Nom *");
        phone.setPromptText("Téléphone");
        email.setPromptText("Email");
        save.getStyleClass().add("button-primary");
        save.setOnAction(e -> save());
        Button cancel = new Button("Annuler");
        cancel.getStyleClass().add("button-ghost");
        cancel.setOnAction(e -> reset());
        boolean canWrite = session.hasPermission("customer.create") || session.hasPermission("customer.update");
        HBox form = new HBox(8, firstName, lastName, phone, email, save, cancel);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setVisible(canWrite);
        form.setManaged(canWrite);
        search.setPromptText("Rechercher un client");
        search.setOnAction(e -> searchNow());
        Button searchBtn = new Button("Rechercher");
        searchBtn.getStyleClass().add("button-secondary");
        searchBtn.setOnAction(e -> searchNow());
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());
        HBox searchBar = new HBox(8, search, searchBtn, refresh);
        HBox.setHgrow(search, Priority.ALWAYS);
        VBox card = new VBox(10, form, searchBar);
        card.getStyleClass().add("card");
        table.setItems(FXCollections.observableArrayList());
        table.setPlaceholder(new EmptyState("Aucun client"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("Nom", Customer::displayName),
                col("Téléphone", Customer::phone),
                col("Email", Customer::email),
                col("Points", c -> String.valueOf(c.loyaltyPoints()))
        );
        TableColumn<Customer, Void> actions = new TableColumn<>();
        actions.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Customer customer = getTableRow().getItem();
                HBox box = new HBox(6);
                if (session.hasPermission("customer.update")) {
                    Button edit = new Button("Modifier");
                    edit.getStyleClass().add("button-ghost");
                    edit.setOnAction(e -> beginEdit(customer));
                    box.getChildren().add(edit);
                }
                if (session.hasPermission("customer.delete")) {
                    Button del = new Button("Suppr.");
                    del.getStyleClass().add("button-danger");
                    del.setOnAction(e -> delete(customer));
                    box.getChildren().add(del);
                }
                setGraphic(box.getChildren().isEmpty() ? null : box);
            }
        });
        table.getColumns().add(actions);
        VBox page = new VBox(16, title, sub, error, card, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void beginEdit(Customer c) {
        editingId = c.id();
        firstName.setText(c.firstName());
        lastName.setText(c.lastName());
        phone.setText(c.phone());
        email.setText(c.email());
        save.setText("Mettre à jour");
    }

    private void reset() {
        editingId = null;
        firstName.clear();
        lastName.clear();
        phone.clear();
        email.clear();
        save.setText("Créer");
    }

    private void save() {
        if (blank(firstName) || blank(lastName)) {
            error.show("Nom et prénom obligatoires.");
            return;
        }
        Customer body = new Customer(editingId, firstName.getText().trim(), lastName.getText().trim(),
                phone.getText(), email.getText(), "", "", "", 0);
        loading.setLoading(true);
        if (editingId == null) {
            FxAsync.run(() -> client.create(body), ignored -> { reset(); reload(); }, this::fail);
        } else {
            Long id = editingId;
            FxAsync.run(() -> client.update(id, body), ignored -> { reset(); reload(); }, this::fail);
        }
    }

    private void delete(Customer c) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer", "Supprimer ce client ?")) {
            return;
        }
        loading.setLoading(true);
        FxAsync.runVoid(() -> client.delete(c.id()), this::reload, this::fail);
    }

    private void searchNow() {
        String q = search.getText() == null ? "" : search.getText().trim();
        loading.setLoading(true);
        if (q.isEmpty()) {
            reload();
            return;
        }
        FxAsync.run(() -> client.search(q), list -> {
            loading.setLoading(false);
            table.getItems().setAll(list);
        }, this::fail);
    }

    public void reload() {
        loading.setLoading(true);
        FxAsync.run(client::list, list -> {
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

    private static TableColumn<Customer, String> col(String title, java.util.function.Function<Customer, String> fn) {
        TableColumn<Customer, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
