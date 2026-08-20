package com.gestpov.desktop.ui.customers;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.Customer;
import com.gestpov.desktop.model.CustomerHistory;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.CustomerClient;
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

public final class CustomersView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final CustomerClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<Customer> table = new TableView<>();
    private final ListPager<Customer> pager = new ListPager<>(table);
    private final TextField firstName = new TextField();
    private final TextField lastName = new TextField();
    private final TextField phone = new TextField();
    private final TextField email = new TextField();
    private final TextField companyName = new TextField();
    private final TextField address = new TextField();
    private final TextField city = new TextField();
    private final TextField search = new TextField();
    private final Button save = new Button("Créer");
    private final Label countLabel = new Label();
    private final Label detailTitle = new Label("Sélectionnez un client");
    private final Label detailStats = new Label();
    private final TableView<CustomerHistory.LoyaltyTxn> txnTable = new TableView<>();
    private final TextField adjustPoints = new TextField();
    private final TextField adjustReason = new TextField();
    private final VBox detailPanel = new VBox(10);
    private Long editingId;
    private Customer selected;

    public CustomersView(SessionContext session) {
        this.session = session;
        this.client = new CustomerClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Clients");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Fiches clients, historique et fidélité");
        sub.getStyleClass().add("page-sub");
        countLabel.getStyleClass().add("page-sub");

        firstName.setPromptText("Prénom *");
        lastName.setPromptText("Nom *");
        phone.setPromptText("Téléphone");
        email.setPromptText("Email");
        companyName.setPromptText("Société");
        address.setPromptText("Adresse");
        city.setPromptText("Ville");

        save.getStyleClass().add("button-primary");
        save.setOnAction(e -> save());
        Button cancel = new Button("Annuler");
        cancel.getStyleClass().add("button-ghost");
        cancel.setOnAction(e -> reset());

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(25);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(25);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(25);
        ColumnConstraints c4 = new ColumnConstraints();
        c4.setPercentWidth(25);
        formGrid.getColumnConstraints().addAll(c1, c2, c3, c4);
        formGrid.add(firstName, 0, 0);
        formGrid.add(lastName, 1, 0);
        formGrid.add(phone, 2, 0);
        formGrid.add(email, 3, 0);
        formGrid.add(companyName, 0, 1);
        formGrid.add(city, 1, 1);
        formGrid.add(address, 2, 1, 2, 1);
        HBox actions = new HBox(8, save, cancel);
        actions.setAlignment(Pos.CENTER_LEFT);
        formGrid.add(actions, 0, 2, 4, 1);

        boolean canWrite = session.hasPermission("customer.create") || session.hasPermission("customer.update");
        formGrid.setVisible(canWrite);
        formGrid.setManaged(canWrite);

        search.setPromptText("Rechercher un client (nom, tel, email, société…)");
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

        table.setPlaceholder(new EmptyState("Aucun client — créez-en un ci-dessus"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("Nom", Customer::displayName),
                col("Société", c -> blankToDash(c.companyName())),
                col("Téléphone", Customer::phone),
                col("Email", Customer::email),
                col("Ville", c -> blankToDash(c.city())),
                col("Points", c -> String.valueOf(c.loyaltyPoints() == null ? 0 : c.loyaltyPoints()))
        );
        TableColumn<Customer, Void> actionsCol = new TableColumn<>();
        actionsCol.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Customer customer = getTableRow().getItem();
                HBox box = new HBox(6);
                Button detail = new Button("Détail");
                detail.getStyleClass().add("button-ghost");
                detail.setOnAction(e -> loadDetail(customer));
                box.getChildren().add(detail);
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
                setGraphic(box);
            }
        });
        table.getColumns().add(actionsCol);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                loadDetail(b);
            }
        });

        buildDetailPanel();

        HBox split = new HBox(16, table, detailPanel);
        HBox.setHgrow(table, Priority.ALWAYS);
        detailPanel.setPrefWidth(340);
        detailPanel.setMinWidth(280);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox page = new VBox(16, title, sub, countLabel, error, card, split, pager.bar());
        VBox.setVgrow(split, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void buildDetailPanel() {
        detailTitle.getStyleClass().add("settings-group-title");
        detailStats.getStyleClass().add("page-sub");
        detailStats.setWrapText(true);
        txnTable.setPlaceholder(new EmptyState("Aucune transaction"));
        txnTable.setPrefHeight(160);
        txnTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        txnTable.getColumns().add(colTxn("Type", t -> t.type() == null ? "" : t.type()));
        txnTable.getColumns().add(colTxn("Points", t -> String.valueOf(t.points() == null ? 0 : t.points())));
        txnTable.getColumns().add(colTxn("Date", t -> t.createdAt() == null ? "" : t.createdAt()));

        adjustPoints.setPromptText("Points (+/-)");
        adjustReason.setPromptText("Motif");
        Button adjust = new Button("Ajuster points");
        adjust.getStyleClass().add("button-secondary");
        adjust.setOnAction(e -> adjustLoyalty());
        boolean canLoyalty = session.hasPermission("loyalty.manage");
        HBox loyaltyForm = new HBox(8, adjustPoints, adjustReason, adjust);
        loyaltyForm.setVisible(canLoyalty);
        loyaltyForm.setManaged(canLoyalty);
        HBox.setHgrow(adjustReason, Priority.ALWAYS);

        detailPanel.getChildren().setAll(detailTitle, detailStats, loyaltyForm, new Label("Transactions récentes"), txnTable);
        detailPanel.getStyleClass().add("card");
        detailPanel.setPadding(new Insets(12));
    }

    private void loadDetail(Customer customer) {
        selected = customer;
        detailTitle.setText(customer.displayName());
        detailStats.setText("Chargement…");
        FxAsync.run(() -> client.history(customer.id()), history -> {
            if (history == null) {
                detailStats.setText("Pas d'historique");
                txnTable.getItems().clear();
                return;
            }
            detailStats.setText(String.format(
                    "N° %s · Points %s (%s)%nAchats %d · CA %s · Panier moy. %s%nDernier achat %s · Gagnés %d / Utilisés %d",
                    blankToDash(history.customerNumber()),
                    history.loyaltyPoints() == null ? 0 : history.loyaltyPoints(),
                    blankToDash(history.loyaltyTier()),
                    history.purchaseCount(),
                    history.totalSpent() == null ? "—" : history.totalSpent().toPlainString(),
                    history.averageBasket() == null ? "—" : history.averageBasket().toPlainString(),
                    blankToDash(history.lastPurchaseAt()),
                    history.totalPointsEarned() == null ? 0 : history.totalPointsEarned(),
                    history.totalPointsRedeemed() == null ? 0 : history.totalPointsRedeemed()
            ));
            txnTable.getItems().setAll(history.recentTransactions() == null
                    ? java.util.List.of() : history.recentTransactions());
        }, this::fail);
    }

    private void adjustLoyalty() {
        if (selected == null) {
            error.show("Sélectionnez un client.");
            return;
        }
        int points;
        try {
            points = Integer.parseInt(adjustPoints.getText().trim());
        } catch (Exception e) {
            error.show("Points invalides.");
            return;
        }
        String reason = adjustReason.getText() == null ? "" : adjustReason.getText().trim();
        loading.setLoading(true);
        Long id = selected.id();
        FxAsync.run(() -> client.adjustPoints(id, points, reason), updated -> {
            adjustPoints.clear();
            adjustReason.clear();
            reload();
            loadDetail(updated);
        }, this::fail);
    }

    private void beginEdit(Customer c) {
        editingId = c.id();
        firstName.setText(c.firstName());
        lastName.setText(c.lastName());
        phone.setText(c.phone());
        email.setText(c.email());
        companyName.setText(c.companyName());
        address.setText(c.address());
        city.setText(c.city());
        save.setText("Mettre à jour");
    }

    private void reset() {
        editingId = null;
        firstName.clear();
        lastName.clear();
        phone.clear();
        email.clear();
        companyName.clear();
        address.clear();
        city.clear();
        save.setText("Créer");
    }

    private void save() {
        if (blank(firstName) || blank(lastName)) {
            error.show("Nom et prénom obligatoires.");
            return;
        }
        String mail = email.getText() == null ? "" : email.getText().trim();
        if (!mail.isEmpty() && !mail.contains("@")) {
            error.show("Email invalide.");
            return;
        }
        Customer body = new Customer(
                editingId,
                firstName.getText().trim(),
                lastName.getText().trim(),
                trimOrEmpty(phone),
                mail,
                trimOrEmpty(companyName),
                trimOrEmpty(address),
                trimOrEmpty(city),
                0
        );
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
            pager.setItems(list);
            countLabel.setText(list.size() + " client(s)");
        }, this::fail);
    }

    @Override
    public void reload() {
        loading.setLoading(true);
        FxAsync.run(client::list, list -> {
            loading.setLoading(false);
            pager.setItems(list);
            countLabel.setText(list.size() + " client(s)");
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

    private static String trimOrEmpty(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static TableColumn<Customer, String> col(String title, java.util.function.Function<Customer, String> fn) {
        TableColumn<Customer, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static TableColumn<CustomerHistory.LoyaltyTxn, String> colTxn(
            String title, java.util.function.Function<CustomerHistory.LoyaltyTxn, String> fn) {
        TableColumn<CustomerHistory.LoyaltyTxn, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
