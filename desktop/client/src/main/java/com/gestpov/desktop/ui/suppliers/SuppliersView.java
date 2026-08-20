package com.gestpov.desktop.ui.suppliers;

import com.gestpov.desktop.model.Supplier;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.SupplierClient;
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

public final class SuppliersView extends StackPane {

    private final SessionContext session;
    private final SupplierClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<Supplier> table = new TableView<>();
    private final TextField nom = new TextField();
    private final TextField email = new TextField();
    private final TextField telephone = new TextField();
    private final TextField adresse = new TextField();
    private final TextField search = new TextField();
    private Long editingId;

    public SuppliersView(SessionContext session) {
        this.session = session;
        this.client = new SupplierClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Fournisseurs");
        title.getStyleClass().add("page-title");
        nom.setPromptText("Nom *");
        email.setPromptText("Email");
        telephone.setPromptText("Téléphone");
        adresse.setPromptText("Adresse");
        Button save = new Button("Créer");
        save.getStyleClass().add("button-primary");
        save.setOnAction(e -> {
            this.save();
            save.setText(editingId == null ? "Créer" : "Mettre à jour");
        });
        Button cancel = new Button("Annuler");
        cancel.getStyleClass().add("button-ghost");
        cancel.setOnAction(e -> reset());
        boolean canWrite = session.hasPermission("products.create") || session.hasPermission("products.update");
        HBox form = new HBox(8, nom, email, telephone, adresse, save, cancel);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setVisible(canWrite);
        form.setManaged(canWrite);
        HBox.setHgrow(nom, Priority.ALWAYS);
        search.setPromptText("Rechercher un fournisseur");
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
        table.setPlaceholder(new EmptyState("Aucun fournisseur"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(col("Nom", Supplier::nom), col("Email", Supplier::email),
                col("Téléphone", Supplier::telephone), col("Adresse", Supplier::adresse));
        if (session.hasPermission("products.update") || session.hasPermission("products.delete")) {
            TableColumn<Supplier, Void> actions = new TableColumn<>();
            actions.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        return;
                    }
                    Supplier s = getTableRow().getItem();
                    HBox box = new HBox(6);
                    if (session.hasPermission("products.update")) {
                        Button edit = new Button("Modifier");
                        edit.getStyleClass().add("button-ghost");
                        edit.setOnAction(e -> beginEdit(s));
                        box.getChildren().add(edit);
                    }
                    if (session.hasPermission("products.delete")) {
                        Button del = new Button("Suppr.");
                        del.getStyleClass().add("button-danger");
                        del.setOnAction(e -> delete(s));
                        box.getChildren().add(del);
                    }
                    setGraphic(box);
                }
            });
            table.getColumns().add(actions);
        }
        Label sub = new Label("Gestion des partenaires");
        sub.getStyleClass().add("page-sub");
        VBox page = new VBox(16, title, sub, error, card, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void beginEdit(Supplier s) {
        editingId = s.id();
        nom.setText(s.nom());
        email.setText(s.email());
        telephone.setText(s.telephone());
        adresse.setText(s.adresse());
    }

    private void reset() {
        editingId = null;
        nom.clear();
        email.clear();
        telephone.clear();
        adresse.clear();
    }

    private void save() {
        if (nom.getText() == null || nom.getText().trim().isEmpty()) {
            error.show("Le nom du fournisseur est obligatoire.");
            return;
        }
        Supplier body = new Supplier(editingId, nom.getText().trim(), email.getText(), telephone.getText(), adresse.getText());
        loading.setLoading(true);
        if (editingId == null) {
            FxAsync.run(() -> client.create(body), ignored -> { reset(); reload(); }, this::fail);
        } else {
            Long id = editingId;
            FxAsync.run(() -> client.update(id, body), ignored -> { reset(); reload(); }, this::fail);
        }
    }

    private void delete(Supplier s) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer", "Supprimer ce fournisseur ?")) {
            return;
        }
        loading.setLoading(true);
        FxAsync.runVoid(() -> client.delete(s.id()), this::reload, this::fail);
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

    private static TableColumn<Supplier, String> col(String title, java.util.function.Function<Supplier, String> fn) {
        TableColumn<Supplier, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
