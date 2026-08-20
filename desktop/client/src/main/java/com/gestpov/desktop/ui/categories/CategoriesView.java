package com.gestpov.desktop.ui.categories;

import com.gestpov.desktop.model.Category;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.CategoryClient;
import com.gestpov.desktop.service.CategoryValidator;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Parité fonctionnelle Web /categories : arbre, recherche, CRUD, sous-catégorie.
 * Rattachement via PUT parentId (API existante ; le Web ne l'expose pas dans l'UI).
 */
public final class CategoriesView extends StackPane {

    private final SessionContext session;
    private final CategoryClient categories;
    private final TreeView<Category> tree = new TreeView<>();
    private final StackPane treePane = new StackPane();
    private final ListView<Category> searchList = new ListView<>();
    private final ObservableList<Category> searchRows = FXCollections.observableArrayList();
    private final ErrorBanner errorBanner = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final EmptyState emptyState = new EmptyState("Aucune catégorie — créez-en une ci-dessus.");
    private final TextField newName = new TextField();
    private final TextField search = new TextField();
    private final Button createButton = new Button("Créer");
    private final Button searchButton = new Button("Rechercher");
    private final Button refreshButton = new Button("Actualiser");
    private final Button clearSearchButton = new Button("Retour à l'arborescence");
    private final Label searchHint = new Label("Résultats de recherche");
    private boolean searchMode;
    private List<Category> lastTree = List.of();
    private Long editingId;
    private Long addingChildId;
    private Long reattachingId;
    private String draftName = "";
    private String childDraftName = "";

    public CategoriesView(SessionContext session) {
        this.session = session;
        this.categories = new CategoryClient(session.api());
        getChildren().addAll(buildContent(), loading);
        reload();
    }

    private BorderPane buildContent() {
        Label title = new Label("Catégories");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Organisation hiérarchique du catalogue");
        sub.getStyleClass().add("page-sub");

        newName.setPromptText("Nouvelle catégorie racine");
        newName.setOnAction(e -> createRoot());
        createButton.getStyleClass().add("button-primary");
        createButton.setOnAction(e -> createRoot());
        boolean canCreate = session.hasPermission("products.create");
        createButton.setVisible(canCreate);
        createButton.setManaged(canCreate);
        newName.setVisible(canCreate);
        newName.setManaged(canCreate);

        HBox createBar = new HBox(8, newName, createButton);
        HBox.setHgrow(newName, Priority.ALWAYS);
        createBar.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("Rechercher une catégorie");
        search.setOnAction(e -> searchCategories());
        searchButton.getStyleClass().add("button-secondary");
        searchButton.setOnAction(e -> searchCategories());
        refreshButton.getStyleClass().add("button-secondary");
        refreshButton.setOnAction(e -> reload());
        clearSearchButton.getStyleClass().add("button-ghost");
        clearSearchButton.setOnAction(e -> exitSearch());
        clearSearchButton.setVisible(false);
        clearSearchButton.setManaged(false);

        HBox searchBar = new HBox(8, search, searchButton, refreshButton, clearSearchButton);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, createBar, searchBar);
        card.getStyleClass().add("card");

        tree.setShowRoot(false);
        tree.setCellFactory(view -> new CategoryCell());

        searchHint.getStyleClass().add("page-sub");
        searchHint.setVisible(false);
        searchHint.setManaged(false);
        searchList.setItems(searchRows);
        searchList.setCellFactory(list -> new SearchResultCell());
        searchList.setVisible(false);
        searchList.setManaged(false);
        searchList.setPlaceholder(new EmptyState("Aucun résultat"));

        emptyState.setMouseTransparent(true);
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        treePane.getChildren().setAll(tree, emptyState);
        StackPane.setAlignment(emptyState, Pos.CENTER);

        VBox listBox = new VBox(8, searchHint, treePane, searchList);
        VBox.setVgrow(treePane, Priority.ALWAYS);
        VBox.setVgrow(searchList, Priority.ALWAYS);

        VBox page = new VBox(16, title, sub, errorBanner, card, listBox);
        VBox.setVgrow(listBox, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return new BorderPane(page);
    }

    public void reload() {
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(categories::getTree, this::showTree, this::showError);
    }

    private void searchCategories() {
        String q = search.getText() == null ? "" : search.getText().trim();
        if (q.isEmpty()) {
            exitSearch();
            return;
        }
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> categories.search(q), list -> {
            searchMode = true;
            clearSearchButton.setVisible(true);
            clearSearchButton.setManaged(true);
            showSearch(list);
        }, this::showError);
    }

    private void exitSearch() {
        search.clear();
        searchMode = false;
        clearSearchButton.setVisible(false);
        clearSearchButton.setManaged(false);
        reload();
    }

    private void createRoot() {
        create(newName.getText(), null, () -> newName.clear());
    }

    private void create(String rawName, Long parentId, Runnable onSuccess) {
        String nom = CategoryValidator.normalizeName(rawName);
        String local = CategoryValidator.validateName(nom);
        if (local != null) {
            errorBanner.show(local);
            return;
        }
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> categories.create(nom, parentId), created -> {
            addingChildId = null;
            childDraftName = "";
            if (onSuccess != null) {
                onSuccess.run();
            }
            reload();
        }, error -> {
            setBusy(false);
            showError(error);
        });
    }

    private void saveEdit(Category category, String rawName) {
        String nom = CategoryValidator.normalizeName(rawName);
        String local = CategoryValidator.validateName(nom);
        if (local != null) {
            errorBanner.show(local);
            return;
        }
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> categories.update(category.id(), nom, category.parentId()), updated -> {
            editingId = null;
            draftName = "";
            reload();
        }, error -> {
            setBusy(false);
            draftName = nom;
            showError(error);
        });
    }

    private void reattach(Category category, Long newParentId) {
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> categories.update(category.id(), category.nom(), newParentId), updated -> {
            reattachingId = null;
            reload();
        }, error -> {
            setBusy(false);
            showError(error);
        });
    }

    private void delete(Category category) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer la catégorie",
                "Supprimer cette catégorie ?")) {
            return;
        }
        errorBanner.hide();
        setBusy(true);
        FxAsync.runVoid(() -> categories.delete(category.id()), this::reload, this::showError);
    }

    private void showTree(List<Category> roots) {
        setBusy(false);
        lastTree = roots == null ? List.of() : roots;
        searchMode = false;
        searchHint.setVisible(false);
        searchHint.setManaged(false);
        searchList.setVisible(false);
        searchList.setManaged(false);
        treePane.setVisible(true);
        treePane.setManaged(true);
        TreeItem<Category> hidden = new TreeItem<>();
        hidden.setExpanded(true);
        for (Category root : lastTree) {
            hidden.getChildren().add(toItem(root));
        }
        tree.setRoot(hidden);
        boolean empty = lastTree.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        emptyState.setMessage("Aucune catégorie — créez-en une ci-dessus.");
    }

    private static TreeItem<Category> toItem(Category category) {
        TreeItem<Category> item = new TreeItem<>(category);
        item.setExpanded(true);
        for (Category child : category.childrenOrEmpty()) {
            item.getChildren().add(toItem(child));
        }
        return item;
    }

    private void showSearch(List<Category> list) {
        setBusy(false);
        searchHint.setVisible(true);
        searchHint.setManaged(true);
        searchList.setVisible(true);
        searchList.setManaged(true);
        treePane.setVisible(false);
        treePane.setManaged(false);
        searchRows.setAll(list);
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
        tree.setDisable(busy);
        treePane.setDisable(busy);
        searchList.setDisable(busy);
    }

    private void startEdit(Category category) {
        editingId = category.id();
        addingChildId = null;
        reattachingId = null;
        draftName = category.nom();
        tree.refresh();
    }

    private void startAddChild(Category category) {
        addingChildId = category.id();
        editingId = null;
        reattachingId = null;
        childDraftName = "";
        tree.refresh();
    }

    private void startReattach(Category category) {
        reattachingId = category.id();
        editingId = null;
        addingChildId = null;
        tree.refresh();
    }

    private void cancelInline() {
        editingId = null;
        addingChildId = null;
        reattachingId = null;
        draftName = "";
        childDraftName = "";
        tree.refresh();
    }

    private final class CategoryCell extends TreeCell<Category> {
        @Override
        protected void updateItem(Category category, boolean empty) {
            super.updateItem(category, empty);
            if (empty || category == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            if (editingId != null && editingId.equals(category.id()) && session.hasPermission("products.update")) {
                setGraphic(editRow(category));
                return;
            }
            VBox box = new VBox(6);
            box.getChildren().add(actionsRow(category));
            if (addingChildId != null && addingChildId.equals(category.id()) && session.hasPermission("products.create")) {
                box.getChildren().add(addChildRow(category));
            }
            if (reattachingId != null && reattachingId.equals(category.id()) && session.hasPermission("products.update")) {
                box.getChildren().add(reattachRow(category));
            }
            setGraphic(box);
        }

        private HBox editRow(Category category) {
            TextField field = new TextField(draftName == null || draftName.isBlank() ? category.nom() : draftName);
            Button ok = new Button("OK");
            ok.getStyleClass().add("button-secondary");
            Button cancel = new Button("Annuler");
            cancel.getStyleClass().add("button-ghost");
            ok.setOnAction(e -> saveEdit(category, field.getText()));
            cancel.setOnAction(e -> cancelInline());
            HBox box = new HBox(6, field, ok, cancel);
            box.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(field, Priority.ALWAYS);
            return box;
        }

        private HBox addChildRow(Category category) {
            TextField field = new TextField(childDraftName);
            field.setPromptText("Nom sous-catégorie");
            Button add = new Button("Ajouter");
            add.getStyleClass().add("button-primary");
            Button cancel = new Button("Annuler");
            cancel.getStyleClass().add("button-ghost");
            add.setOnAction(e -> {
                childDraftName = field.getText();
                create(field.getText(), category.id(), null);
            });
            cancel.setOnAction(e -> cancelInline());
            HBox box = new HBox(6, field, add, cancel);
            box.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(field, Priority.ALWAYS);
            return box;
        }

        private HBox reattachRow(Category category) {
            ComboBox<ParentChoice> combo = new ComboBox<>();
            combo.getItems().add(ParentChoice.root());
            for (Category option : Category.flatten(lastTree)) {
                if (option.id() != null && option.id().equals(category.id())) {
                    continue;
                }
                combo.getItems().add(ParentChoice.of(option));
            }
            ParentChoice current = category.parentId() == null
                    ? ParentChoice.root()
                    : combo.getItems().stream()
                    .filter(item -> item.id != null && item.id.equals(category.parentId()))
                    .findFirst()
                    .orElse(ParentChoice.root());
            combo.setValue(current);
            Button ok = new Button("OK");
            ok.getStyleClass().add("button-secondary");
            Button cancel = new Button("Annuler");
            cancel.getStyleClass().add("button-ghost");
            ok.setOnAction(e -> {
                ParentChoice chosen = combo.getValue();
                reattach(category, chosen == null ? null : chosen.id);
            });
            cancel.setOnAction(e -> cancelInline());
            HBox box = new HBox(6, combo, ok, cancel);
            box.setAlignment(Pos.CENTER_LEFT);
            return box;
        }

        private HBox actionsRow(Category category) {
            Label name = new Label(category.nom());
            name.getStyleClass().add("category-name");
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER_RIGHT);
            if (session.hasPermission("products.update")) {
                Button edit = new Button("Modifier");
                edit.getStyleClass().add("button-ghost");
                edit.setOnAction(e -> CategoriesView.this.startEdit(category));
                Button move = new Button("Rattacher");
                move.getStyleClass().add("button-ghost");
                move.setOnAction(e -> startReattach(category));
                actions.getChildren().addAll(edit, move);
            }
            if (session.hasPermission("products.create")) {
                Button child = new Button("+ Sous-cat.");
                child.getStyleClass().add("button-ghost");
                child.setOnAction(e -> startAddChild(category));
                actions.getChildren().add(child);
            }
            if (session.hasPermission("products.delete")) {
                Button del = new Button("Suppr.");
                del.getStyleClass().add("button-danger");
                del.setOnAction(e -> delete(category));
                actions.getChildren().add(del);
            }
            HBox row = new HBox(12, name, actions);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(actions, Priority.ALWAYS);
            return row;
        }
    }

    private static final class SearchResultCell extends ListCell<Category> {
        @Override
        protected void updateItem(Category category, boolean empty) {
            super.updateItem(category, empty);
            if (empty || category == null) {
                setText(null);
                return;
            }
            if (category.parentNom() == null || category.parentNom().isBlank()) {
                setText(category.nom());
            } else {
                setText(category.nom() + " (" + category.parentNom() + ")");
            }
        }
    }

    private static final class ParentChoice {
        private final Long id;
        private final String label;

        private ParentChoice(Long id, String label) {
            this.id = id;
            this.label = label;
        }

        static ParentChoice root() {
            return new ParentChoice(null, "— Racine —");
        }

        static ParentChoice of(Category category) {
            String label = category.nom();
            if (category.parentNom() != null && !category.parentNom().isBlank()) {
                label = category.nom() + " (" + category.parentNom() + ")";
            }
            return new ParentChoice(category.id(), label);
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
