package com.gestpov.desktop.ui.brands;

import com.gestpov.desktop.model.Brand;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.BrandClient;
import com.gestpov.desktop.service.BrandValidator;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ConfirmationDialog;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Parité fonctionnelle Web /brands : liste, recherche, CRUD, refresh.
 */
public final class BrandsView extends StackPane {

    private final SessionContext session;
    private final BrandClient brands;
    private final ObservableList<Brand> rows = FXCollections.observableArrayList();
    private final TableView<Brand> table = new TableView<>();
    private final ErrorBanner errorBanner = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final EmptyState emptyState = new EmptyState("Aucune marque — créez-en une ci-dessus.");
    private final TextField newName = new TextField();
    private final TextField search = new TextField();
    private final Button createButton = new Button("Créer");
    private final Button searchButton = new Button("Rechercher");
    private final Button refreshButton = new Button("Actualiser");
    private final Button clearSearchButton = new Button("Retour à la liste");
    private boolean searchMode;
    private Long editingId;
    private String draftName = "";

    public BrandsView(SessionContext session) {
        this.session = session;
        this.brands = new BrandClient(session.api());
        getChildren().addAll(buildContent(), loading);
        reload();
    }

    private BorderPane buildContent() {
        Label title = new Label("Marques");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Liste des marques produits — sélectionnables sur chaque fiche");
        sub.getStyleClass().add("page-sub");

        newName.setPromptText("Nouvelle marque");
        newName.setOnAction(e -> create());
        createButton.getStyleClass().add("button-primary");
        createButton.setOnAction(e -> create());
        createButton.setVisible(session.hasPermission("products.create"));
        createButton.setManaged(session.hasPermission("products.create"));
        newName.setVisible(session.hasPermission("products.create"));
        newName.setManaged(session.hasPermission("products.create"));

        HBox createBar = new HBox(8, newName, createButton);
        HBox.setHgrow(newName, Priority.ALWAYS);
        createBar.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("Rechercher une marque");
        search.setOnAction(e -> searchBrands());
        searchButton.getStyleClass().add("button-secondary");
        searchButton.setOnAction(e -> searchBrands());
        refreshButton.getStyleClass().add("button-secondary");
        refreshButton.setOnAction(e -> reload());
        clearSearchButton.getStyleClass().add("button-ghost");
        clearSearchButton.setOnAction(e -> {
            search.clear();
            searchMode = false;
            clearSearchButton.setVisible(false);
            clearSearchButton.setManaged(false);
            reload();
        });
        clearSearchButton.setVisible(false);
        clearSearchButton.setManaged(false);

        HBox searchBar = new HBox(8, search, searchButton, refreshButton, clearSearchButton);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, createBar, searchBar);
        card.getStyleClass().add("card");

        table.setItems(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(emptyState);
        TableColumn<Brand, String> nameCol = new TableColumn<>("Nom");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue() == null ? "" : data.getValue().nom()));
        nameCol.setSortable(true);
        TableColumn<Brand, Void> actionsCol = new TableColumn<>("");
        actionsCol.setPrefWidth(220);
        actionsCol.setSortable(false);
        actionsCol.setCellFactory(col -> new ActionsCell());
        table.getColumns().add(nameCol);
        table.getColumns().add(actionsCol);
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && session.hasPermission("products.update")) {
                Brand selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    startEdit(selected);
                }
            }
        });

        VBox page = new VBox(16, title, sub, errorBanner, card, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));

        BorderPane root = new BorderPane(page);
        return root;
    }

    public void reload() {
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(brands::findAll, this::showRows, this::showError);
    }

    private void searchBrands() {
        String q = search.getText() == null ? "" : search.getText().trim();
        if (q.isEmpty()) {
            searchMode = false;
            reload();
            return;
        }
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> brands.search(q), list -> {
            searchMode = true;
            clearSearchButton.setVisible(true);
            clearSearchButton.setManaged(true);
            showRows(list);
            if (list.isEmpty()) {
                emptyState.setMessage("Aucun résultat");
            }
        }, this::showError);
    }

    private void create() {
        String nom = BrandValidator.normalizeName(newName.getText());
        String local = BrandValidator.validateName(nom);
        if (local != null) {
            errorBanner.show(local);
            return;
        }
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> brands.create(nom), created -> {
            newName.clear();
            reload();
        }, error -> {
            setBusy(false);
            showError(error);
        });
    }

    private void saveEdit(Brand brand, String nom) {
        String local = BrandValidator.validateName(nom);
        if (local != null) {
            errorBanner.show(local);
            return;
        }
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> brands.update(brand.id(), BrandValidator.normalizeName(nom)), updated -> {
            editingId = null;
            reload();
        }, error -> {
            setBusy(false);
            draftName = nom;
            showError(error);
        });
    }

    private void delete(Brand brand) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer la marque",
                "Supprimer cette marque ?")) {
            return;
        }
        errorBanner.hide();
        setBusy(true);
        FxAsync.runVoid(() -> brands.delete(brand.id()), this::reload, this::showError);
    }

    private void showRows(List<Brand> list) {
        setBusy(false);
        rows.setAll(list);
        if (!searchMode) {
            emptyState.setMessage("Aucune marque — créez-en une ci-dessus.");
        }
        table.refresh();
    }

    private void showError(Throwable error) {
        setBusy(false);
        if (error instanceof ApiException api && api.isUnauthorized()) {
            return;
        }
        if (error instanceof ApiException api) {
            errorBanner.show(ApiException.userMessage(api));
        } else {
            errorBanner.show("Une erreur est survenue.");
        }
    }

    private void setBusy(boolean busy) {
        loading.setLoading(busy);
        createButton.setDisable(busy);
        searchButton.setDisable(busy);
        refreshButton.setDisable(busy);
        newName.setDisable(busy);
        search.setDisable(busy);
        table.setDisable(busy);
    }

    private void startEdit(Brand brand) {
        editingId = brand.id();
        draftName = brand.nom();
        table.refresh();
    }

    private final class ActionsCell extends TableCell<Brand, Void> {
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            Brand brand = getTableRow().getItem();
            if (editingId != null && editingId.equals(brand.id()) && session.hasPermission("products.update")) {
                TextField field = new TextField(draftName == null || draftName.isBlank() ? brand.nom() : draftName);
                Button ok = new Button("OK");
                ok.getStyleClass().add("button-secondary");
                Button cancel = new Button("Annuler");
                cancel.getStyleClass().add("button-ghost");
                ok.setOnAction(e -> saveEdit(brand, field.getText()));
                cancel.setOnAction(e -> {
                    editingId = null;
                    draftName = "";
                    table.refresh();
                });
                HBox box = new HBox(6, field, ok, cancel);
                box.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(field, Priority.ALWAYS);
                setGraphic(box);
                return;
            }
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER_RIGHT);
            if (session.hasPermission("products.update")) {
                Button edit = new Button("Modifier");
                edit.getStyleClass().add("button-ghost");
                edit.setOnAction(e -> BrandsView.this.startEdit(brand));
                actions.getChildren().add(edit);
            }
            if (session.hasPermission("products.delete")) {
                Button del = new Button("Suppr.");
                del.getStyleClass().add("button-danger");
                del.setOnAction(e -> delete(brand));
                actions.getChildren().add(del);
            }
            setGraphic(actions.getChildren().isEmpty() ? null : actions);
        }
    }
}
