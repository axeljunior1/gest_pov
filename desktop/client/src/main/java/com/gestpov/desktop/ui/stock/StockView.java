package com.gestpov.desktop.ui.stock;

import com.gestpov.desktop.model.AppSetting;
import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.StockItem;
import com.gestpov.desktop.model.StockLocation;
import com.gestpov.desktop.model.StockMovement;
import com.gestpov.desktop.model.Supplier;
import com.gestpov.desktop.model.Warehouse;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.ProductClient;
import com.gestpov.desktop.net.SettingsClient;
import com.gestpov.desktop.net.StockClient;
import com.gestpov.desktop.net.SupplierClient;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
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
import java.time.LocalDate;
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
    private final SupplierClient suppliers;
    private final SettingsClient settings;
    private final boolean canAdjust;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private BigDecimal lowThreshold = BigDecimal.TEN;

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
    private final TextField reasonField = new TextField();

    private final TableView<StockMovement> movementsTable = new TableView<>();
    private final ComboBox<ProductOption> historyProductFilter = new ComboBox<>();
    private final ComboBox<Warehouse> historyWarehouseFilter = new ComboBox<>();
    private final DatePicker historyDateFrom = new DatePicker();
    private final DatePicker historyDateTo = new DatePicker();

    public StockView(SessionContext session) {
        this.session = session;
        this.client = new StockClient(session.api());
        this.products = new ProductClient(session.api());
        this.suppliers = new SupplierClient(session.api());
        this.settings = new SettingsClient(session.api());
        this.canAdjust = session.hasPermission("stock.adjust");
        getChildren().addAll(build(), loading);
        loadLowThreshold();
        reloadAll();
    }

    private void loadLowThreshold() {
        FxAsync.run(settings::getAll, list -> {
            for (AppSetting s : list) {
                if ("stock.low_threshold_default".equals(s.key())) {
                    try {
                        lowThreshold = new BigDecimal(s.value().trim());
                    } catch (Exception ignored) {
                        // valeur invalide en base : seuil par defaut conserve
                    }
                    break;
                }
            }
            applyFilter();
        }, ignored -> {
            // seuil par defaut (10) conserve si le chargement echoue
        });
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
        Button orderBtn = new Button("Commander (sélection)");
        orderBtn.getStyleClass().add("button-secondary");
        orderBtn.setOnAction(e -> openCreatePurchaseOrderDialog());
        orderBtn.setVisible(session.hasPermission("stock_entry.create"));
        orderBtn.setManaged(orderBtn.isVisible());
        HBox bar = new HBox(8, search, warehouseFilter, onlyLow, refresh, orderBtn);
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
                } else if (avail.compareTo(lowThreshold) <= 0) {
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
        Label hint = new Label("Réception = entrée, Ajustement = corriger (quantité en unité de base). "
                + "Pour une casse, perte, avarie ou retour fournisseur, utilisez l'onglet Entrées/Sorties.");
        hint.getStyleClass().add("page-sub");

        moveType.getItems().setAll("Réception", "Ajustement");
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
        reasonField.setPromptText("Motif (obligatoire pour un ajustement)");

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
                labeled("Motif", reasonField),
                new HBox(10, submit, reloadRefs)
        );
        form.getStyleClass().add("card");
        movePane.getChildren().setAll(h, hint, form);
    }

    private void buildHistoryPane() {
        Button refresh = new Button("Actualiser l'historique");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reloadMovements());

        historyProductFilter.setPromptText("Tous produits");
        historyProductFilter.setMaxWidth(220);
        historyProductFilter.valueProperty().addListener((o, a, b) -> reloadMovements());
        historyWarehouseFilter.setPromptText("Tous entrepôts");
        historyWarehouseFilter.setMaxWidth(180);
        historyWarehouseFilter.valueProperty().addListener((o, a, b) -> reloadMovements());
        historyDateFrom.setPromptText("Du");
        historyDateFrom.setOnAction(e -> reloadMovements());
        historyDateTo.setPromptText("Au");
        historyDateTo.setOnAction(e -> reloadMovements());
        Button clearFilters = new Button("×");
        clearFilters.getStyleClass().add("button-ghost");
        clearFilters.setTooltip(new javafx.scene.control.Tooltip("Effacer les filtres"));
        clearFilters.setOnAction(e -> {
            historyProductFilter.getSelectionModel().clearSelection();
            historyWarehouseFilter.getSelectionModel().clearSelection();
            historyDateFrom.setValue(null);
            historyDateTo.setValue(null);
            reloadMovements();
        });

        HBox bar = new HBox(8, refresh, historyProductFilter, historyWarehouseFilter,
                historyDateFrom, historyDateTo, clearFilters);
        bar.setAlignment(Pos.CENTER_LEFT);
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
        reasonField.clear();
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
            error.show("Pour une réception, saisissez une quantité positive.");
            return;
        }
        if ("Ajustement".equals(type) && (reasonField.getText() == null || reasonField.getText().isBlank())) {
            error.show("Un motif est obligatoire pour un ajustement de stock.");
            return;
        }
        String ref = referenceField.getText();
        if ("Ajustement".equals(type)) {
            submitAdjustment(product, warehouse, location, qty, ref, null, null);
            return;
        }
        loading.setLoading(true);
        FxAsync.runVoid(() -> client.receipt(product.id(), warehouse.id(), location.id(), qty, ref), () -> {
            loading.setLoading(false);
            qtyField.clear();
            reloadItems();
            tabStock.setSelected(true);
            showSelectedTab();
        }, this::fail);
    }

    private void submitAdjustment(ProductOption product, Warehouse warehouse, StockLocation location,
                                  BigDecimal qty, String ref, String managerEmail, String managerPassword) {
        String reason = reasonField.getText();
        loading.setLoading(true);
        FxAsync.runVoid(() -> client.adjust(product.id(), warehouse.id(), location.id(), qty, ref,
                reason, managerEmail, managerPassword), () -> {
            loading.setLoading(false);
            qtyField.clear();
            reasonField.clear();
            reloadItems();
            tabStock.setSelected(true);
            showSelectedTab();
        }, t -> {
            loading.setLoading(false);
            if (t instanceof ApiException api && !api.isUnauthorized()
                    && ApiException.userMessage(api).contains("Validation manager obligatoire")) {
                promptManagerApproval(product, warehouse, location, qty, ref);
            } else {
                fail(t);
            }
        });
    }

    private void promptManagerApproval(ProductOption product, Warehouse warehouse, StockLocation location,
                                       BigDecimal qty, String ref) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Validation manager requise");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        Label info = new Label("Cet ajustement (" + qty.stripTrailingZeros().toPlainString()
                + ") dépasse le seuil autorisé — validation d'un manager obligatoire.");
        info.setWrapText(true);
        info.getStyleClass().add("page-sub");
        TextField managerEmail = new TextField();
        managerEmail.setPromptText("Email manager");
        javafx.scene.control.PasswordField managerPassword = new javafx.scene.control.PasswordField();
        managerPassword.setPromptText("Mot de passe manager");
        Label dialogError = new Label();
        dialogError.getStyleClass().add("error-banner-text");
        dialogError.setVisible(false);
        dialogError.setManaged(false);

        Button confirm = new Button("Valider l'ajustement");
        confirm.getStyleClass().add("button-primary");
        confirm.setOnAction(e -> {
            if (managerEmail.getText() == null || managerEmail.getText().isBlank()
                    || managerPassword.getText() == null || managerPassword.getText().isBlank()) {
                dialogError.setText("Email et mot de passe manager obligatoires.");
                dialogError.setVisible(true);
                dialogError.setManaged(true);
                return;
            }
            dialog.close();
            submitAdjustment(product, warehouse, location, qty, ref,
                    managerEmail.getText().trim(), managerPassword.getText());
        });

        VBox content = new VBox(10, info, dialogError,
                labeled("Email", managerEmail), labeled("Mot de passe", managerPassword), confirm);
        content.setPadding(new Insets(12));
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void openCreatePurchaseOrderDialog() {
        StockItem item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            error.show("Sélectionnez une ligne de stock.");
            return;
        }
        if (item.productId() == null) {
            error.show("Produit non identifié pour cette ligne.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(suppliers::findAll, list -> {
            loading.setLoading(false);
            if (list.isEmpty()) {
                error.show("Créez d'abord un fournisseur (onglet Fournisseurs).");
                return;
            }
            showCreatePurchaseOrderDialog(item, list);
        }, this::fail);
    }

    private void showCreatePurchaseOrderDialog(StockItem item, List<Supplier> supplierList) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Commander « " + (item.productNom() == null ? "produit" : item.productNom()) + " »");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        ComboBox<Supplier> supplierCombo = new ComboBox<>();
        supplierCombo.getItems().setAll(supplierList);
        supplierCombo.setMaxWidth(Double.MAX_VALUE);
        supplierCombo.getSelectionModel().selectFirst();

        ComboBox<Warehouse> whCombo = new ComboBox<>();
        whCombo.getItems().setAll(warehouseCombo.getItems());
        whCombo.setMaxWidth(Double.MAX_VALUE);
        if (item.warehouseId() != null) {
            whCombo.getItems().stream().filter(w -> item.warehouseId().equals(w.id())).findFirst()
                    .ifPresent(w -> whCombo.getSelectionModel().select(w));
        }
        if (whCombo.getValue() == null && !whCombo.getItems().isEmpty()) {
            whCombo.getSelectionModel().selectFirst();
        }

        BigDecimal suggested = lowThreshold.subtract(avail(item));
        if (suggested.compareTo(BigDecimal.ONE) < 0) {
            suggested = BigDecimal.ONE;
        }
        TextField qtyField = new TextField(suggested.stripTrailingZeros().toPlainString());
        DatePicker deliveryDate = new DatePicker(LocalDate.now().plusDays(7));
        deliveryDate.setMaxWidth(Double.MAX_VALUE);
        TextField notesField = new TextField();
        notesField.setPromptText("Notes (optionnel)");

        Button confirm = new Button("Créer la commande");
        confirm.getStyleClass().add("button-primary");
        confirm.setOnAction(e -> {
            Supplier supplier = supplierCombo.getValue();
            if (supplier == null || supplier.id() == null) {
                error.show("Choisissez un fournisseur.");
                return;
            }
            BigDecimal qty;
            try {
                qty = new BigDecimal(qtyField.getText().trim().replace(',', '.'));
            } catch (Exception ex) {
                error.show("Quantité invalide.");
                return;
            }
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                error.show("La quantité doit être positive.");
                return;
            }
            Long whId = whCombo.getValue() == null ? null : whCombo.getValue().id();
            LocalDate date = deliveryDate.getValue();
            String notes = notesField.getText();
            long productId = item.productId();
            BigDecimal finalQty = qty;
            loading.setLoading(true);
            FxAsync.run(() -> client.createPurchaseOrder(supplier.id(), whId, date, notes, productId, finalQty),
                    po -> {
                        loading.setLoading(false);
                        error.hide();
                        dialog.close();
                        Alert done = new Alert(Alert.AlertType.INFORMATION,
                                "Commande " + (po.reference() == null ? "" : po.reference()) + " créée.");
                        done.setHeaderText("Bon de commande");
                        done.showAndWait();
                    }, t -> {
                        loading.setLoading(false);
                        fail(t);
                    });
        });

        VBox content = new VBox(10,
                labeled("Fournisseur", supplierCombo),
                labeled("Entrepôt", whCombo),
                labeled("Quantité à commander", qtyField),
                labeled("Livraison prévue", deliveryDate),
                labeled("Notes", notesField),
                confirm
        );
        content.setPadding(new Insets(10));
        content.setPrefWidth(320);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
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
            historyWarehouseFilter.getItems().setAll(refs.warehouses());
            historyProductFilter.getItems().setAll(productCombo.getItems());
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
        ProductOption product = historyProductFilter.getValue();
        Warehouse warehouse = historyWarehouseFilter.getValue();
        Long productId = product == null ? null : product.id();
        Long warehouseId = warehouse == null ? null : warehouse.id();
        LocalDate from = historyDateFrom.getValue();
        LocalDate to = historyDateTo.getValue();
        FxAsync.run(() -> client.listMovements(productId, warehouseId, from, to), list -> {
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
            if (lowOnly && avail(s).compareTo(lowThreshold) > 0) {
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
            return a.compareTo(BigDecimal.ZERO) > 0 && a.compareTo(lowThreshold) <= 0;
        }).count();
        summary.setText(filtered.size() + " ligne(s) · " + rupture + " rupture · " + faible + " stock faible (≤ "
                + lowThreshold.stripTrailingZeros().toPlainString() + ")"
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

    private String statusOf(StockItem s) {
        BigDecimal a = avail(s);
        if (a.compareTo(BigDecimal.ZERO) <= 0) {
            return "Rupture";
        }
        if (a.compareTo(lowThreshold) <= 0) {
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

    private TableColumn<StockItem, String> statusCol() {
        TableColumn<StockItem, String> col = col("Statut", this::statusOf);
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
