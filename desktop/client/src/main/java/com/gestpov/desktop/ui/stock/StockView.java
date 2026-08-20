package com.gestpov.desktop.ui.stock;

import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.StockItem;
import com.gestpov.desktop.model.StockLocation;
import com.gestpov.desktop.model.StockMovement;
import com.gestpov.desktop.model.Warehouse;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.ProductClient;
import com.gestpov.desktop.net.StockClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.Reloadable;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.ListPager;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stock autonome Desktop : liste, réception / sortie / ajustement, historique.
 */
public final class StockView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final StockClient client;
    private final ProductClient products;
    private final boolean canAdjust;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();

    private final ToggleButton tabStock = new ToggleButton("Stock");
    private final ToggleButton tabMove = new ToggleButton("Mouvement");
    private final ToggleButton tabHistory = new ToggleButton("Historique");

    private final VBox stockPane = new VBox(12);
    private final VBox movePane = new VBox(12);
    private final VBox historyPane = new VBox(12);

    private final TableView<StockItem> table = new TableView<>();
    private final ListPager<StockItem> pager = new ListPager<>(table);
    private final TextField search = new TextField();
    private final ComboBox<String> warehouseFilter = new ComboBox<>();
    private final CheckBox onlyLow = new CheckBox("Stock faible / rupture");
    private final Label summary = new Label();
    private List<StockItem> all = List.of();

    private final ComboBox<String> moveType = new ComboBox<>();
    private final ComboBox<ProductOption> productCombo = new ComboBox<>();
    private final ComboBox<Warehouse> warehouseCombo = new ComboBox<>();
    private final ComboBox<StockLocation> locationCombo = new ComboBox<>();
    private final TextField qtyField = new TextField();
    private final TextField referenceField = new TextField();

    private final TableView<StockMovement> movementsTable = new TableView<>();

    public StockView(SessionContext session) {
        this.session = session;
        this.client = new StockClient(session.api());
        this.products = new ProductClient(session.api());
        this.canAdjust = session.hasPermission("stock.adjust");
        getChildren().addAll(build(), loading);
        reloadAll();
    }

    private VBox build() {
        Label title = new Label("Stock");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Produits du catalogue (qty 0 si jamais réceptionnés) · mouvements · historique");
        sub.getStyleClass().add("page-sub");
        summary.getStyleClass().add("page-sub");

        ToggleGroup tabs = new ToggleGroup();
        styleTab(tabStock, tabs, true);
        styleTab(tabMove, tabs, false);
        styleTab(tabHistory, tabs, false);
        tabMove.setDisable(!canAdjust);
        tabMove.setVisible(canAdjust);
        tabMove.setManaged(canAdjust);
        HBox tabBar = new HBox(8, tabStock, tabMove, tabHistory);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabs.selectedToggleProperty().addListener((o, a, b) -> showSelectedTab());

        buildStockPane();
        buildMovePane();
        buildHistoryPane();
        showSelectedTab();

        StackPane body = new StackPane(stockPane, movePane, historyPane);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox page = new VBox(14, title, sub, error, tabBar, body);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void styleTab(ToggleButton btn, ToggleGroup group, boolean selected) {
        btn.setToggleGroup(group);
        btn.getStyleClass().add("button-secondary");
        btn.setSelected(selected);
        btn.setOnAction(e -> {
            if (!btn.isSelected()) {
                btn.setSelected(true);
            }
            showSelectedTab();
        });
    }

    private void showSelectedTab() {
        stockPane.setVisible(tabStock.isSelected());
        stockPane.setManaged(tabStock.isSelected());
        movePane.setVisible(tabMove.isSelected());
        movePane.setManaged(tabMove.isSelected());
        historyPane.setVisible(tabHistory.isSelected());
        historyPane.setManaged(tabHistory.isSelected());
        if (tabHistory.isSelected()) {
            reloadMovements();
        }
    }

    private void buildStockPane() {
        search.setPromptText("Filtrer par produit, emplacement…");
        search.textProperty().addListener((o, a, b) -> applyFilter());
        warehouseFilter.setPromptText("Tous entrepôts");
        warehouseFilter.getItems().add("Tous entrepôts");
        warehouseFilter.getSelectionModel().selectFirst();
        warehouseFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        onlyLow.setOnAction(e -> applyFilter());
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reloadItems());
        HBox bar = new HBox(8, search, warehouseFilter, onlyLow, refresh);
        bar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        VBox card = new VBox(10, bar, summary);
        card.getStyleClass().add("card");

        table.setPlaceholder(new EmptyState("Aucun stock — utilisez l'onglet Mouvement pour réceptionner"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(StockItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    return;
                }
                BigDecimal avail = avail(item);
                if (avail.compareTo(BigDecimal.ZERO) <= 0) {
                    setStyle("-fx-background-color: #fee2e2;");
                } else if (avail.compareTo(BigDecimal.TEN) <= 0) {
                    setStyle("-fx-background-color: #fef3c7;");
                } else {
                    setStyle("");
                }
            }
        });
        table.getColumns().addAll(
                col("Produit", s -> s.productNom() == null ? "—" : s.productNom()),
                col("Entrepôt", s -> s.warehouseCode() == null ? "—" : s.warehouseCode()),
                col("Emplacement", s -> s.locationCode() == null ? "—" : s.locationCode()),
                qtyCol("En stock", StockItem::quantityOnHand),
                qtyCol("Disponible", StockItem::quantityAvailable),
                statusCol(),
                col("Unité", s -> s.unitSymbole() == null ? "" : s.unitSymbole())
        );
        if (canAdjust) {
            table.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    StockItem item = table.getSelectionModel().getSelectedItem();
                    if (item != null) {
                        prefillMoveFrom(item);
                        tabMove.setSelected(true);
                        showSelectedTab();
                    }
                }
            });
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        stockPane.getChildren().setAll(card, table, pager.bar());
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void buildMovePane() {
        Label h = new Label("Enregistrer un mouvement");
        h.getStyleClass().add("settings-group-title");
        Label hint = new Label("Réception = entrée, Sortie = sortie, Ajustement = corriger (quantité en unité de base).");
        hint.getStyleClass().add("page-sub");

        moveType.getItems().setAll("Réception", "Sortie", "Ajustement");
        moveType.getSelectionModel().selectFirst();
        productCombo.setPromptText("Produit");
        productCombo.setMaxWidth(Double.MAX_VALUE);
        warehouseCombo.setPromptText("Entrepôt");
        warehouseCombo.setMaxWidth(Double.MAX_VALUE);
        warehouseCombo.valueProperty().addListener((o, a, b) -> loadLocations(b));
        locationCombo.setPromptText("Emplacement");
        locationCombo.setMaxWidth(Double.MAX_VALUE);
        qtyField.setPromptText("Quantité (unité de base)");
        referenceField.setPromptText("Référence (bon, facture…)");

        Button submit = new Button("Enregistrer");
        submit.getStyleClass().add("button-primary");
        submit.setOnAction(e -> submitMove());
        Button reloadRefs = new Button("Recharger listes");
        reloadRefs.getStyleClass().add("button-secondary");
        reloadRefs.setOnAction(e -> reloadMoveRefs());

        VBox form = new VBox(10,
                labeled("Type", moveType),
                labeled("Produit", productCombo),
                labeled("Entrepôt", warehouseCombo),
                labeled("Emplacement", locationCombo),
                labeled("Quantité", qtyField),
                labeled("Référence", referenceField),
                new HBox(10, submit, reloadRefs)
        );
        form.getStyleClass().add("card");
        movePane.getChildren().setAll(h, hint, form);
    }

    private void buildHistoryPane() {
        Button refresh = new Button("Actualiser l'historique");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reloadMovements());
        HBox bar = new HBox(refresh);
        bar.getStyleClass().add("card");
        movementsTable.setPlaceholder(new EmptyState("Aucun mouvement"));
        movementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        movementsTable.setItems(FXCollections.observableArrayList());
        movementsTable.getColumns().addAll(
                movCol("Date", m -> m.createdAt() == null ? "—" : m.createdAt()),
                movCol("Type", StockMovement::typeLabel),
                movCol("Produit", m -> m.productNom() == null ? "—" : m.productNom()),
                movCol("Entrepôt", m -> m.warehouseCode() == null ? "—" : m.warehouseCode()),
                movCol("Empl.", m -> m.locationCode() == null ? "—" : m.locationCode()),
                movCol("Qté", m -> m.quantity() == null ? "—" : m.quantity().stripTrailingZeros().toPlainString()),
                movCol("Après", m -> m.quantityAfter() == null ? "—" : m.quantityAfter().stripTrailingZeros().toPlainString()),
                movCol("Réf.", m -> m.reference() == null ? "" : m.reference())
        );
        VBox.setVgrow(movementsTable, Priority.ALWAYS);
        historyPane.getChildren().setAll(bar, movementsTable);
    }

    private void prefillMoveFrom(StockItem item) {
        moveType.getSelectionModel().select("Réception");
        qtyField.clear();
        referenceField.clear();
        if (item.productId() != null) {
            productCombo.getItems().stream()
                    .filter(p -> item.productId().equals(p.id()))
                    .findFirst()
                    .ifPresent(p -> productCombo.getSelectionModel().select(p));
        }
        if (item.warehouseId() != null) {
            warehouseCombo.getItems().stream()
                    .filter(w -> item.warehouseId().equals(w.id()))
                    .findFirst()
                    .ifPresent(w -> {
                        warehouseCombo.getSelectionModel().select(w);
                        loadLocations(w);
                        if (item.locationId() != null) {
                            locationCombo.getItems().stream()
                                    .filter(l -> item.locationId().equals(l.id()))
                                    .findFirst()
                                    .ifPresent(l -> locationCombo.getSelectionModel().select(l));
                        }
                    });
        }
    }

    private void submitMove() {
        error.hide();
        ProductOption product = productCombo.getValue();
        Warehouse warehouse = warehouseCombo.getValue();
        StockLocation location = locationCombo.getValue();
        if (product == null || product.id() == null) {
            error.show("Choisissez un produit.");
            return;
        }
        if (warehouse == null || warehouse.id() == null) {
            error.show("Choisissez un entrepôt.");
            return;
        }
        if (location == null || location.id() == null) {
            error.show("Choisissez un emplacement.");
            return;
        }
        BigDecimal qty;
        try {
            qty = new BigDecimal(qtyField.getText().trim().replace(',', '.'));
        } catch (Exception e) {
            error.show("Quantité invalide.");
            return;
        }
        if (qty.compareTo(BigDecimal.ZERO) == 0) {
            error.show("La quantité ne peut pas être zéro.");
            return;
        }
        String type = moveType.getValue() == null ? "Réception" : moveType.getValue();
        if (!"Ajustement".equals(type) && qty.compareTo(BigDecimal.ZERO) < 0) {
            error.show("Pour une réception ou une sortie, saisissez une quantité positive.");
            return;
        }
        String ref = referenceField.getText();
        loading.setLoading(true);
        FxAsync.runVoid(() -> {
            switch (type) {
                case "Sortie" -> client.issue(product.id(), warehouse.id(), location.id(), qty, ref);
                case "Ajustement" -> client.adjust(product.id(), warehouse.id(), location.id(), qty, ref);
                default -> client.receipt(product.id(), warehouse.id(), location.id(), qty, ref);
            }
        }, () -> {
            loading.setLoading(false);
            qtyField.clear();
            reloadItems();
            tabStock.setSelected(true);
            showSelectedTab();
        }, this::fail);
    }

    @Override
    public void reload() {
        reloadAll();
    }

    private void reloadAll() {
        reloadItems();
        reloadMoveRefs();
        if (tabHistory.isSelected()) {
            reloadMovements();
        }
    }

    private void reloadItems() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> {
            List<StockItem> items = new ArrayList<>(client.listItems());
            List<Product> catalog = products.search(new com.gestpov.desktop.model.ProductQuery());
            Set<Long> withStock = new HashSet<>();
            for (StockItem item : items) {
                if (item.productId() != null) {
                    withStock.add(item.productId());
                }
            }
            for (Product p : catalog) {
                if (p == null || p.id() == null || withStock.contains(p.id())) {
                    continue;
                }
                String unit = p.unitSymbole() != null && !p.unitSymbole().isBlank()
                        ? p.unitSymbole()
                        : p.baseUnitSymbole();
                items.add(new StockItem(
                        null,
                        p.id(),
                        p.nom(),
                        null,
                        "—",
                        null,
                        null,
                        unit,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ));
            }
            items.sort(Comparator
                    .comparing((StockItem s) -> s.productNom() == null ? "" : s.productNom(),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(s -> s.warehouseCode() == null ? "" : s.warehouseCode(),
                            String.CASE_INSENSITIVE_ORDER));
            return items;
        }, list -> {
            loading.setLoading(false);
            all = list;
            refreshWarehouseFilter();
            applyFilter();
        }, this::fail);
    }

    private void reloadMoveRefs() {
        if (!canAdjust) {
            return;
        }
        FxAsync.run(() -> {
            List<Warehouse> wh = client.listWarehouses();
            List<Product> prods = products.search(new com.gestpov.desktop.model.ProductQuery());
            return new MoveRefs(wh, prods);
        }, refs -> {
            warehouseCombo.getItems().setAll(refs.warehouses());
            if (!refs.warehouses().isEmpty() && warehouseCombo.getValue() == null) {
                warehouseCombo.getSelectionModel().selectFirst();
                loadLocations(warehouseCombo.getValue());
            }
            productCombo.getItems().setAll(refs.products().stream()
                    .map(p -> new ProductOption(p.id(), p.nom(), p.sku()))
                    .toList());
        }, ignored -> {
            // listes optionnelles au démarrage
        });
    }

    private void loadLocations(Warehouse warehouse) {
        locationCombo.getItems().clear();
        if (warehouse == null || warehouse.id() == null) {
            return;
        }
        FxAsync.run(() -> client.listLocations(warehouse.id()), locs -> {
            locationCombo.getItems().setAll(locs);
            if (!locs.isEmpty()) {
                locationCombo.getSelectionModel().selectFirst();
            }
        }, this::fail);
    }

    private void reloadMovements() {
        loading.setLoading(true);
        FxAsync.run(client::listMovements, list -> {
            loading.setLoading(false);
            movementsTable.getItems().setAll(list);
        }, this::fail);
    }

    private void refreshWarehouseFilter() {
        String selected = warehouseFilter.getValue();
        List<String> codes = all.stream()
                .map(StockItem::warehouseCode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        warehouseFilter.getItems().setAll("Tous entrepôts");
        warehouseFilter.getItems().addAll(codes);
        if (selected != null && warehouseFilter.getItems().contains(selected)) {
            warehouseFilter.getSelectionModel().select(selected);
        } else {
            warehouseFilter.getSelectionModel().selectFirst();
        }
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        String wh = warehouseFilter.getValue();
        boolean lowOnly = onlyLow.isSelected();
        List<StockItem> filtered = all.stream().filter(s -> {
            if (wh != null && !"Tous entrepôts".equals(wh)
                    && (s.warehouseCode() == null || !wh.equalsIgnoreCase(s.warehouseCode()))) {
                return false;
            }
            if (lowOnly && avail(s).compareTo(BigDecimal.TEN) > 0) {
                return false;
            }
            if (q.isEmpty()) {
                return true;
            }
            return (s.productNom() != null && s.productNom().toLowerCase(Locale.ROOT).contains(q))
                    || (s.warehouseCode() != null && s.warehouseCode().toLowerCase(Locale.ROOT).contains(q))
                    || (s.locationCode() != null && s.locationCode().toLowerCase(Locale.ROOT).contains(q));
        }).collect(Collectors.toList());
        pager.setItems(filtered);
        long rupture = filtered.stream().filter(s -> avail(s).compareTo(BigDecimal.ZERO) <= 0).count();
        long faible = filtered.stream().filter(s -> {
            BigDecimal a = avail(s);
            return a.compareTo(BigDecimal.ZERO) > 0 && a.compareTo(BigDecimal.TEN) <= 0;
        }).count();
        summary.setText(filtered.size() + " ligne(s) · " + rupture + " rupture · " + faible + " stock faible (≤ 10)"
                + (canAdjust ? " · Double-clic = mouvement" : ""));
    }

    private void fail(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api && !api.isUnauthorized()) {
            error.show(ApiException.userMessage(api));
        } else if (!(t instanceof ApiException a && a.isUnauthorized())) {
            error.show("Une erreur est survenue.");
        }
    }

    private static VBox labeled(String title, javafx.scene.Node node) {
        Label l = new Label(title);
        l.getStyleClass().add("form-label");
        return new VBox(4, l, node);
    }

    private static BigDecimal avail(StockItem s) {
        return s.quantityAvailable() == null ? BigDecimal.ZERO : s.quantityAvailable();
    }

    private static String statusOf(StockItem s) {
        BigDecimal a = avail(s);
        if (a.compareTo(BigDecimal.ZERO) <= 0) {
            return "Rupture";
        }
        if (a.compareTo(BigDecimal.TEN) <= 0) {
            return "Faible";
        }
        return "OK";
    }

    private static TableColumn<StockItem, String> col(String title, java.util.function.Function<StockItem, String> fn) {
        TableColumn<StockItem, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static TableColumn<StockItem, String> qtyCol(String title, java.util.function.Function<StockItem, BigDecimal> fn) {
        return col(title, s -> {
            BigDecimal v = fn.apply(s);
            return v == null ? "0" : v.stripTrailingZeros().toPlainString();
        });
    }

    private static TableColumn<StockItem, String> statusCol() {
        TableColumn<StockItem, String> col = col("Statut", StockView::statusOf);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (empty || item == null) {
                    setStyle("");
                } else if ("Rupture".equals(item)) {
                    setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: 800;");
                } else if ("Faible".equals(item)) {
                    setStyle("-fx-text-fill: #b45309; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #166534; -fx-font-weight: 700;");
                }
            }
        });
        return col;
    }

    private static TableColumn<StockMovement, String> movCol(String title,
                                                            java.util.function.Function<StockMovement, String> fn) {
        TableColumn<StockMovement, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private record MoveRefs(List<Warehouse> warehouses, List<Product> products) {
    }

    private record ProductOption(Long id, String nom, String sku) {
        @Override
        public String toString() {
            if (sku == null || sku.isBlank()) {
                return nom == null ? "" : nom;
            }
            return nom + " (" + sku + ")";
        }
    }
}
