package com.gestpov.desktop.ui.suppliers;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.PurchaseOrder;
import com.gestpov.desktop.model.Supplier;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.StockClient;
import com.gestpov.desktop.net.SupplierClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ConfirmationDialog;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public final class SuppliersView extends StackPane implements Reloadable {

    private static final List<String> PENDING_STATUSES = List.of("PENDING", "PARTIALLY_RECEIVED");

    private final SessionContext session;
    private final SupplierClient client;
    private final StockClient stockClient;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<Supplier> table = new TableView<>();
    private final ListPager<Supplier> pager = new ListPager<>(table);
    private final TextField nom = new TextField();
    private final TextField email = new TextField();
    private final TextField telephone = new TextField();
    private final TextField adresse = new TextField();
    private final TextField search = new TextField();
    private final Button save = new Button("Créer");
    private final Label countLabel = new Label();
    private final Label ordersTitle = new Label("Sélectionnez un fournisseur");
    private final TableView<PurchaseOrder> ordersTable = new TableView<>();
    private final VBox ordersPanel = new VBox(10);
    private Long editingId;

    public SuppliersView(SessionContext session) {
        this.session = session;
        this.client = new SupplierClient(session.api());
        this.stockClient = new StockClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Fournisseurs");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Gestion des partenaires d'achat");
        sub.getStyleClass().add("page-sub");
        countLabel.getStyleClass().add("page-sub");

        nom.setPromptText("Nom *");
        email.setPromptText("Email");
        telephone.setPromptText("Téléphone");
        adresse.setPromptText("Adresse");

        save.getStyleClass().add("button-primary");
        save.setOnAction(e -> save());
        Button cancel = new Button("Annuler");
        cancel.getStyleClass().add("button-ghost");
        cancel.setOnAction(e -> reset());

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(8);
        ColumnConstraints a = new ColumnConstraints();
        a.setPercentWidth(30);
        ColumnConstraints b = new ColumnConstraints();
        b.setPercentWidth(25);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(20);
        ColumnConstraints d = new ColumnConstraints();
        d.setPercentWidth(25);
        formGrid.getColumnConstraints().addAll(a, b, c, d);
        formGrid.add(nom, 0, 0);
        formGrid.add(email, 1, 0);
        formGrid.add(telephone, 2, 0);
        formGrid.add(adresse, 3, 0);
        HBox actions = new HBox(8, save, cancel);
        actions.setAlignment(Pos.CENTER_LEFT);
        formGrid.add(actions, 0, 1, 4, 1);

        boolean canWrite = session.hasPermission("products.create") || session.hasPermission("products.update");
        formGrid.setVisible(canWrite);
        formGrid.setManaged(canWrite);

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

        VBox card = new VBox(12, formGrid, searchBar);
        card.getStyleClass().add("card");

        table.setPlaceholder(new EmptyState("Aucun fournisseur — créez-en un ci-dessus"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("Nom", Supplier::nom),
                col("Email", Supplier::email),
                col("Téléphone", Supplier::telephone),
                col("Adresse", Supplier::adresse)
        );
        if (session.hasPermission("products.update") || session.hasPermission("products.delete")) {
            TableColumn<Supplier, Void> actionsCol = new TableColumn<>();
            actionsCol.setCellFactory(cell -> new javafx.scene.control.TableCell<>() {
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
            table.getColumns().add(actionsCol);
        }
        boolean canSeeOrders = session.hasPermission("stock_entry.read");
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (canSeeOrders) {
                loadOrders(sel);
            }
        });

        buildOrdersPanel();
        ordersPanel.setVisible(canSeeOrders);
        ordersPanel.setManaged(canSeeOrders);

        HBox split = new HBox(16, table, ordersPanel);
        HBox.setHgrow(table, Priority.ALWAYS);
        ordersPanel.setPrefWidth(340);
        ordersPanel.setMinWidth(280);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox page = new VBox(16, title, sub, countLabel, error, card, split, pager.bar());
        VBox.setVgrow(split, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void buildOrdersPanel() {
        ordersTitle.getStyleClass().add("settings-group-title");
        ordersTable.setPlaceholder(new EmptyState("Aucune commande en cours"));
        ordersTable.setPrefHeight(240);
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ordersTable.getColumns().addAll(
                poCol("Référence", p -> nz(p.reference())),
                poCol("Statut", p -> nz(p.status())),
                poCol("Livraison prévue", p -> nz(p.expectedDeliveryDate()))
        );
        ordersPanel.getChildren().setAll(ordersTitle, new Label("Commandes en cours"), ordersTable);
        ordersPanel.getStyleClass().add("card");
        ordersPanel.setPadding(new Insets(12));
    }

    private void loadOrders(Supplier supplier) {
        ordersTable.getItems().clear();
        if (supplier == null || supplier.id() == null) {
            ordersTitle.setText("Sélectionnez un fournisseur");
            return;
        }
        ordersTitle.setText(supplier.nom());
        FxAsync.run(() -> stockClient.listPurchaseOrders(null, supplier.id()), list -> {
            List<PurchaseOrder> pending = list.stream()
                    .filter(p -> p.status() != null && PENDING_STATUSES.contains(p.status()))
                    .toList();
            ordersTable.getItems().setAll(pending);
        }, this::fail);
    }

    private void beginEdit(Supplier s) {
        editingId = s.id();
        nom.setText(s.nom());
        email.setText(s.email());
        telephone.setText(s.telephone());
        adresse.setText(s.adresse());
        save.setText("Mettre à jour");
    }

    private void reset() {
        editingId = null;
        nom.clear();
        email.clear();
        telephone.clear();
        adresse.clear();
        save.setText("Créer");
    }

    private void save() {
        if (nom.getText() == null || nom.getText().trim().isEmpty()) {
            error.show("Le nom du fournisseur est obligatoire.");
            return;
        }
        Supplier body = new Supplier(editingId, nom.getText().trim(),
                trim(email), trim(telephone), trim(adresse));
        loading.setLoading(true);
        if (editingId == null) {
            FxAsync.run(() -> client.create(body), ignored -> {
                reset();
                reload();
            }, this::fail);
        } else {
            Long id = editingId;
            FxAsync.run(() -> client.update(id, body), ignored -> {
                reset();
                reload();
            }, this::fail);
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
            pager.setItems(list);
            countLabel.setText(list.size() + " fournisseur(s)");
        }, this::fail);
    }

    @Override
    public void reload() {
        loading.setLoading(true);
        FxAsync.run(client::findAll, list -> {
            loading.setLoading(false);
            pager.setItems(list);
            countLabel.setText(list.size() + " fournisseur(s)");
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

    private static String trim(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static TableColumn<Supplier, String> col(String title, java.util.function.Function<Supplier, String> fn) {
        TableColumn<Supplier, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static TableColumn<PurchaseOrder, String> poCol(String title,
                                                            java.util.function.Function<PurchaseOrder, String> fn) {
        TableColumn<PurchaseOrder, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
