package com.gestpov.desktop.ui.products;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.Brand;
import com.gestpov.desktop.model.Category;
import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.ProductQuery;
import com.gestpov.desktop.model.Supplier;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.BrandClient;
import com.gestpov.desktop.net.CategoryClient;
import com.gestpov.desktop.net.ProductClient;
import com.gestpov.desktop.net.SupplierClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ConfirmationDialog;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.ListPager;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongConsumer;

public final class ProductsView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final ProductClient products;
    private final CategoryClient categories;
    private final SupplierClient suppliers;
    private final Runnable onCreate;
    private final LongConsumer onOpen;
    private final ObservableList<Product> rows = FXCollections.observableArrayList();
    private final Set<Long> selectedIds = new HashSet<>();
    private final TableView<Product> table = new TableView<>();
    private final ListPager<Product> pager = new ListPager<>(table);
    private final ErrorBanner errorBanner = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final EmptyState emptyState = new EmptyState("Aucun produit — créez-en un avec « Nouveau produit »");
    private final Label countLabel = new Label();
    private final TextField query = new TextField();
    private final ComboBox<FilterOption> categoryFilter = new ComboBox<>();
    private final ComboBox<FilterOption> supplierFilter = new ComboBox<>();
    private final ComboBox<FilterOption> brandFilter = new ComboBox<>();
    private final BrandClient brands;
    private final ComboBox<FilterOption> lifecycleFilter = new ComboBox<>();
    private final CheckBox stockFaible = new CheckBox("Stock faible");
    private final CheckBox rupture = new CheckBox("Rupture");
    private final Button filterButton = new Button("Filtrer");
    private final Button createButton = new Button("Nouveau produit");
    private final Button bulkButton = new Button("Supprimer la sélection");
    private final Label selectedLabel = new Label();
    private final HBox bulkBar = new HBox(12);

    public ProductsView(SessionContext session, Runnable onCreate, LongConsumer onOpen) {
        this.session = session;
        this.products = new ProductClient(session.api());
        this.categories = new CategoryClient(session.api());
        this.suppliers = new SupplierClient(session.api());
        this.brands = new BrandClient(session.api());
        this.onCreate = onCreate;
        this.onOpen = onOpen;
        getChildren().addAll(buildContent(), loading);
        loadFilters();
        reload();
    }

    private BorderPane buildContent() {
        Label title = new Label("Produits");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Catalogue et fiches produits — double-clic pour ouvrir");
        sub.getStyleClass().add("page-sub");
        countLabel.getStyleClass().add("page-sub");

        createButton.getStyleClass().add("button-primary");
        createButton.setOnAction(e -> onCreate.run());
        boolean canCreate = session.hasPermission("products.create");
        createButton.setVisible(canCreate);
        createButton.setManaged(canCreate);

        HBox header = new HBox(12, title, createButton);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        query.setPromptText("Rechercher un produit");
        query.setOnAction(e -> reload());
        brandFilter.setPromptText("Toutes marques");
        categoryFilter.setPromptText("Toutes catégories");
        supplierFilter.setPromptText("Tous fournisseurs");
        lifecycleFilter.getItems().add(FilterOption.all("Tous cycles de vie"));
        for (String cycle : ProductLabels.CYCLES) {
            lifecycleFilter.getItems().add(new FilterOption(cycle, ProductLabels.lifecycle(cycle)));
        }
        lifecycleFilter.getSelectionModel().selectFirst();
        filterButton.getStyleClass().add("button-secondary");
        filterButton.setOnAction(e -> reload());
        stockFaible.setOnAction(e -> reload());
        rupture.setOnAction(e -> reload());

        HBox row1 = new HBox(8, query, categoryFilter, supplierFilter);
        HBox.setHgrow(query, Priority.ALWAYS);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(8, brandFilter, lifecycleFilter, stockFaible, rupture, filterButton);
        row2.setAlignment(Pos.CENTER_LEFT);
        VBox filters = new VBox(10, row1, row2);
        filters.getStyleClass().add("card");

        bulkButton.getStyleClass().add("button-danger");
        bulkButton.setOnAction(e -> bulkDelete());
        Button clearSel = new Button("Tout désélectionner");
        clearSel.getStyleClass().add("button-secondary");
        clearSel.setOnAction(e -> {
            selectedIds.clear();
            refreshSelection();
        });
        selectedLabel.getStyleClass().add("page-sub");
        bulkBar.getChildren().addAll(selectedLabel, clearSel, bulkButton);
        bulkBar.setAlignment(Pos.CENTER_LEFT);
        bulkBar.getStyleClass().add("card");
        bulkBar.setVisible(false);
        bulkBar.setManaged(false);

                table.setPlaceholder(emptyState);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        if (session.hasPermission("products.delete")) {
            TableColumn<Product, Boolean> selCol = new TableColumn<>();
            selCol.setPrefWidth(42);
            selCol.setMaxWidth(42);
            selCol.setSortable(false);
            selCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(
                    data.getValue() != null && selectedIds.contains(data.getValue().id())));
            selCol.setCellFactory(col -> new TableCell<>() {
                private final CheckBox box = new CheckBox();

                {
                    box.setOnAction(e -> {
                        Product product = getTableRow() == null ? null : getTableRow().getItem();
                        if (product == null || product.id() == null) {
                            return;
                        }
                        if (box.isSelected()) {
                            selectedIds.add(product.id());
                        } else {
                            selectedIds.remove(product.id());
                        }
                        refreshSelection();
                    });
                }

                @Override
                protected void updateItem(Boolean selected, boolean empty) {
                    super.updateItem(selected, empty);
                    if (empty) {
                        setGraphic(null);
                        return;
                    }
                    box.setSelected(Boolean.TRUE.equals(selected));
                    setGraphic(box);
                }
            });
            table.getColumns().add(selCol);
        }
        table.getColumns().add(textCol("Produit", p -> {
            String nom = p.nom() == null ? "" : p.nom();
            if (p.marque() == null || p.marque().isBlank()) {
                return nom;
            }
            return nom + "\n" + p.marque();
        }));
        table.getColumns().add(textCol("SKU", p -> p.sku() == null ? "" : p.sku()));
        table.getColumns().add(textCol("Catégorie", p -> p.categorieNom() == null ? "—" : p.categorieNom()));
        table.getColumns().add(textCol("Prix vente", p -> ProductLabels.price(p.prixVente())));
        TableColumn<Product, String> stockCol = textCol("Stock", Product::stockLabel);
        stockCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                Product product = getTableRow() == null ? null : getTableRow().getItem();
                if (!empty && product != null && (product.stockTotal() == null || product.stockTotal() == 0)) {
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
                } else {
                    setStyle("");
                }
            }
        });
        table.getColumns().add(stockCol);
        table.getColumns().add(textCol("Statut", p -> ProductLabels.status(p.statut())));
        table.getColumns().add(textCol("Cycle de vie", p -> ProductLabels.lifecycle(p.cycleVie())));
        if (session.hasPermission("products.delete")) {
            TableColumn<Product, Void> actions = new TableColumn<>();
            actions.setPrefWidth(90);
            actions.setSortable(false);
            actions.setCellFactory(col -> new TableCell<>() {
                private final Button del = new Button("Suppr.");

                {
                    del.getStyleClass().add("button-danger");
                    del.setOnAction(e -> {
                        Product product = getTableRow() == null ? null : getTableRow().getItem();
                        if (product != null) {
                            deleteOne(product);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : del);
                }
            });
            table.getColumns().add(actions);
        }
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2) {
                return;
            }
            Object target = e.getTarget();
            String type = target == null ? "" : target.getClass().getName();
            if (type.contains("CheckBox") || type.contains("Button")) {
                return;
            }
            Product selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && selected.id() != null) {
                onOpen.accept(selected.id());
            }
        });

        VBox page = new VBox(16, header, sub, countLabel, errorBanner, filters, bulkBar, table, pager.bar());
        VBox.setVgrow(table, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return new BorderPane(page);
    }

    private static TableColumn<Product, String> textCol(String title, java.util.function.Function<Product, String> value) {
        TableColumn<Product, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue() == null ? "" : value.apply(data.getValue())));
        return col;
    }

    @Override
    public void reload() {
        errorBanner.hide();
        setBusy(true);
        ProductQuery q = currentQuery();
        FxAsync.run(() -> products.search(q), list -> {
            setBusy(false);
            selectedIds.clear();
            rows.setAll(list);
            pager.setItems(list);
            countLabel.setText(list.size() + " produit(s)");
            refreshSelection();
        }, this::showError);
    }

    private void loadFilters() {
        FxAsync.run(() -> {
            FilterData data = new FilterData();
            data.categories = categories.getTree();
            data.suppliers = suppliers.findAll();
            data.brands = brands.findAll();
            return data;
        }, data -> {
            categoryFilter.getItems().setAll(FilterOption.all("Toutes catégories"));
            flattenCats(data.categories, "").forEach(opt -> categoryFilter.getItems().add(opt));
            categoryFilter.getSelectionModel().selectFirst();
            supplierFilter.getItems().setAll(FilterOption.all("Tous fournisseurs"));
            for (Supplier supplier : data.suppliers) {
                supplierFilter.getItems().add(new FilterOption(String.valueOf(supplier.id()), supplier.nom()));
            }
            supplierFilter.getSelectionModel().selectFirst();
            brandFilter.getItems().setAll(FilterOption.all("Toutes marques"));
            for (Brand brand : data.brands) {
                brandFilter.getItems().add(new FilterOption(brand.nom(), brand.nom()));
            }
            brandFilter.getSelectionModel().selectFirst();
        }, ignored -> {
            // filtres optionnels
        });
    }

    private ProductQuery currentQuery() {
        ProductQuery q = new ProductQuery();
        q.query = query.getText();
        FilterOption brand = brandFilter.getValue();
        if (brand != null && brand.id != null) {
            q.marque = brand.id;
        }
        FilterOption cat = categoryFilter.getValue();
        if (cat != null && cat.id != null) {
            q.categorieId = Long.parseLong(cat.id);
        }
        FilterOption sup = supplierFilter.getValue();
        if (sup != null && sup.id != null) {
            q.fournisseurId = Long.parseLong(sup.id);
        }
        FilterOption cycle = lifecycleFilter.getValue();
        if (cycle != null && cycle.id != null) {
            q.cycleVie = cycle.id;
        }
        q.stockFaible = stockFaible.isSelected() ? Boolean.TRUE : null;
        q.rupture = rupture.isSelected() ? Boolean.TRUE : null;
        return q;
    }

    private void deleteOne(Product product) {
        if (!ConfirmationDialog.confirm(window(), "Supprimer le produit",
                "Supprimer « " + product.nom() + " » ? Cette action est irréversible.")) {
            return;
        }
        setBusy(true);
        FxAsync.runVoid(() -> products.delete(product.id()), this::reload, this::showError);
    }

    private void bulkDelete() {
        if (selectedIds.isEmpty()) {
            return;
        }
        int n = selectedIds.size();
        String message = n == 1
                ? "Supprimer le produit sélectionné ? Cette action est irréversible."
                : "Supprimer " + n + " produits ? Cette action est irréversible.";
        if (!ConfirmationDialog.confirm(window(), "Supprimer la sélection", message)) {
            return;
        }
        List<Long> ids = new ArrayList<>(selectedIds);
        setBusy(true);
        FxAsync.run(() -> products.bulkDelete(ids), ignored -> reload(), this::showError);
    }

    private void refreshSelection() {
        boolean show = session.hasPermission("products.delete") && !selectedIds.isEmpty();
        bulkBar.setVisible(show);
        bulkBar.setManaged(show);
        selectedLabel.setText(selectedIds.size() + " produit(s) sélectionné(s)");
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
        filterButton.setDisable(busy);
        createButton.setDisable(busy);
        table.setDisable(busy);
    }

    private javafx.stage.Window window() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private static List<FilterOption> flattenCats(List<Category> cats, String prefix) {
        List<FilterOption> out = new ArrayList<>();
        if (cats == null) {
            return out;
        }
        for (Category cat : cats) {
            String label = prefix.isEmpty() ? cat.nom() : prefix + " > " + cat.nom();
            out.add(new FilterOption(String.valueOf(cat.id()), label));
            out.addAll(flattenCats(cat.childrenOrEmpty(), label));
        }
        return out;
    }

    private static final class FilterData {
        List<Category> categories = List.of();
        List<Supplier> suppliers = List.of();
        List<Brand> brands = List.of();
    }

    static final class FilterOption {
        final String id;
        final String label;

        FilterOption(String id, String label) {
            this.id = id;
            this.label = label;
        }

        static FilterOption all(String label) {
            return new FilterOption(null, label);
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
