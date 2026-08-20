package com.gestpov.desktop.ui.pos;

import com.gestpov.desktop.model.PosProduct;
import com.gestpov.desktop.model.Sale;
import com.gestpov.desktop.model.SaleLine;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.PosClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.ui.products.ProductLabels;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;

/**
 * POS cœur : session, recherche produit, panier, quantités, remise, paiement, ticket.
 */
public final class PosView extends StackPane {

    private final SessionContext session;
    private final PosClient pos;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TextField openingCash = new TextField("0");
    private final Button openSession = new Button("Ouvrir la caisse");
    private final VBox sessionBar = new VBox(8);
    private final TextField search = new TextField();
    private final ListView<PosProduct> results = new ListView<>();
    private final TableView<SaleLine> cart = new TableView<>();
    private final Label total = new Label("0,00 €");
    private final TextField qty = new TextField("1");
    private final TextField discount = new TextField("0");
    private final ComboBox<String> payMethod = new ComboBox<>();
    private final TextField cashReceived = new TextField();
    private Sale sale;

    public PosView(SessionContext session) {
        this.session = session;
        this.pos = new PosClient(session.api());
        getChildren().addAll(build(), loading);
        boot();
    }

    private VBox build() {
        Label title = new Label("Caisse");
        title.getStyleClass().addAll("page-title", "pos-title");
        Label sub = new Label("F2 recherche · Entrée ajouter · Double-clic sur un résultat");
        sub.getStyleClass().add("page-sub");

        openingCash.getStyleClass().add("pos-input");
        openingCash.setPromptText("Fond de caisse");
        openSession.getStyleClass().addAll("button-primary", "pos-action");
        openSession.setOnAction(e -> open());
        Label sessionLabel = new Label("Ouverture de caisse");
        sessionLabel.getStyleClass().add("pos-section-label");
        sessionBar.getChildren().add(new HBox(12, sessionLabel, openingCash, openSession));
        sessionBar.getStyleClass().addAll("card", "pos-session-card");
        HBox.setHgrow(openingCash, Priority.ALWAYS);

        search.getStyleClass().add("pos-search");
        search.setPromptText("Nom, SKU ou code-barres…");
        search.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                searchNow();
            }
        });
        Button searchBtn = new Button("Chercher");
        searchBtn.getStyleClass().addAll("button-secondary", "pos-action");
        searchBtn.setOnAction(e -> searchNow());
        HBox searchBar = new HBox(10, search, searchBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        results.getStyleClass().add("pos-results");
        results.setPrefHeight(280);
        results.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PosProduct item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String price = ProductLabels.price(item.unitPrice());
                String sku = item.sku() == null || item.sku().isBlank() ? "" : " · " + item.sku();
                setText(item.nom() + sku + "  —  " + price);
                getStyleClass().setAll("pos-result-cell");
            }
        });
        results.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                addSelected();
            }
        });

        qty.getStyleClass().add("pos-qty");
        qty.setPrefWidth(80);
        Button add = new Button("+ Ajouter");
        add.getStyleClass().addAll("button-primary", "pos-action");
        add.setOnAction(e -> addSelected());
        Label qtyLabel = new Label("Qté");
        qtyLabel.getStyleClass().add("pos-section-label");
        HBox addBar = new HBox(10, qtyLabel, qty, add);
        addBar.setAlignment(Pos.CENTER_LEFT);

        Label searchSection = new Label("Recherche produit");
        searchSection.getStyleClass().add("pos-section-label");
        VBox searchCard = new VBox(12, searchSection, searchBar, results, addBar);
        searchCard.getStyleClass().addAll("card", "pos-panel");
        VBox.setVgrow(results, Priority.ALWAYS);

        cart.getStyleClass().add("pos-cart");
        cart.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cart.getColumns().add(col("Produit", l -> l.productNom() == null ? "" : l.productNom()));
        cart.getColumns().add(col("Qté", l -> l.quantityInput() == null ? "" : l.quantityInput().toPlainString()));
        cart.getColumns().add(col("Prix", l -> ProductLabels.price(l.unitPrice())));
        cart.getColumns().add(col("Total", l -> ProductLabels.price(l.lineTotal())));
        cart.setItems(FXCollections.observableArrayList());
        VBox.setVgrow(cart, Priority.ALWAYS);

        Button setQty = new Button("Modifier qté");
        setQty.getStyleClass().addAll("button-secondary", "pos-action-sm");
        setQty.setOnAction(e -> changeQty());
        discount.getStyleClass().add("pos-qty");
        discount.setPrefWidth(100);
        Button applyDisc = new Button("Remise");
        applyDisc.getStyleClass().addAll("button-secondary", "pos-action-sm");
        applyDisc.setOnAction(e -> applyDiscount());
        applyDisc.setVisible(session.hasPermission("pos.sale.discount"));
        applyDisc.setManaged(session.hasPermission("pos.sale.discount"));

        payMethod.getStyleClass().add("pos-input");
        payMethod.getItems().addAll("CASH", "CARD", "MOBILE_MONEY");
        payMethod.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String value) {
                if (value == null) {
                    return "";
                }
                return switch (value) {
                    case "CASH" -> "Espèces";
                    case "CARD" -> "Carte";
                    case "MOBILE_MONEY" -> "Mobile";
                    default -> value;
                };
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });
        payMethod.getSelectionModel().select("CASH");

        cashReceived.getStyleClass().add("pos-input");
        cashReceived.setPromptText("Reçu client");
        Button pay = new Button("Encaisser");
        pay.getStyleClass().addAll("button-pay", "pos-pay-btn");
        pay.setOnAction(e -> pay());
        pay.setVisible(session.hasPermission("pos.payment.collect")
                || session.hasPermission("pos.sale.validate")
                || session.hasPermission("pos.payment.validate"));
        pay.setManaged(pay.isVisible());
        Button ticket = new Button("Ticket");
        ticket.getStyleClass().addAll("button-secondary", "pos-action-sm");
        ticket.setOnAction(e -> showTicket());

        Label totalCaption = new Label("TOTAL À PAYER");
        totalCaption.getStyleClass().add("pos-total-caption");
        total.getStyleClass().add("pos-total-amount");

        HBox totalBar = new HBox(16, totalCaption, total);
        totalBar.setAlignment(Pos.CENTER_LEFT);
        totalBar.getStyleClass().add("pos-total-bar");

        Label payLabel = new Label("Paiement");
        payLabel.getStyleClass().add("pos-section-label");
        HBox payRow = new HBox(10, payMethod, cashReceived, pay, ticket);
        payRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(cashReceived, Priority.ALWAYS);

        HBox lineActions = new HBox(10, setQty, discount, applyDisc);
        lineActions.setAlignment(Pos.CENTER_LEFT);

        Label cartSection = new Label("Panier");
        cartSection.getStyleClass().add("pos-section-label");
        VBox cartCard = new VBox(12, cartSection, cart, lineActions, totalBar, payLabel, payRow);
        cartCard.getStyleClass().addAll("card", "pos-panel", "pos-cart-panel");
        VBox.setVgrow(cart, Priority.ALWAYS);

        HBox workspace = new HBox(16, searchCard, cartCard);
        workspace.getStyleClass().add("pos-workspace");
        HBox.setHgrow(searchCard, Priority.ALWAYS);
        HBox.setHgrow(cartCard, Priority.ALWAYS);
        searchCard.setPrefWidth(420);
        cartCard.setPrefWidth(520);

        VBox page = new VBox(14, title, sub, error, sessionBar, workspace);
        page.getStyleClass().addAll("content", "pos-page");
        VBox.setVgrow(workspace, Priority.ALWAYS);
        page.setPadding(new Insets(0));

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F2) {
                search.requestFocus();
            }
        });
        return page;
    }

    private void boot() {
        loading.setLoading(true);
        FxAsync.run(pos::context, ctx -> {
            loading.setLoading(false);
            boolean hasSession = ctx.hasNonNull("session") && !ctx.get("session").isNull();
            sessionBar.setVisible(!hasSession);
            sessionBar.setManaged(!hasSession);
            if (hasSession && session.hasPermission("pos.sale.create")) {
                ensureSale();
            }
        }, this::fail);
    }

    private void open() {
        BigDecimal cash;
        try {
            cash = new BigDecimal(openingCash.getText().trim().isEmpty() ? "0" : openingCash.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            error.show("Fond de caisse invalide.");
            return;
        }
        loading.setLoading(true);
        FxAsync.runVoid(() -> pos.openSession(cash), () -> {
            sessionBar.setVisible(false);
            sessionBar.setManaged(false);
            ensureSale();
        }, this::fail);
    }

    private void ensureSale() {
        if (!session.hasPermission("pos.sale.create")) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(pos::createSale, this::showSale, this::fail);
    }

    private void searchNow() {
        String q = search.getText() == null ? "" : search.getText().trim();
        if (q.isEmpty()) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.search(q), list -> {
            loading.setLoading(false);
            results.getItems().setAll(list);
            if (list.size() == 1) {
                results.getSelectionModel().select(0);
                addSelected();
            }
        }, this::fail);
    }

    private void addSelected() {
        PosProduct product = results.getSelectionModel().getSelectedItem();
        if (product == null || sale == null) {
            return;
        }
        BigDecimal quantity;
        try {
            quantity = new BigDecimal(qty.getText().trim().isEmpty() ? "1" : qty.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            error.show("Quantité invalide.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.addLine(sale.id(), product.id(), product.matchedVariantId(), quantity),
                this::showSale, this::fail);
    }

    private void changeQty() {
        SaleLine line = cart.getSelectionModel().getSelectedItem();
        if (line == null || sale == null) {
            error.show("Sélectionnez une ligne.");
            return;
        }
        BigDecimal quantity;
        try {
            quantity = new BigDecimal(qty.getText().trim().replace(',', '.'));
        } catch (Exception e) {
            error.show("Quantité invalide.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.updateQty(sale.id(), line.id(), quantity), this::showSale, this::fail);
    }

    private void applyDiscount() {
        SaleLine line = cart.getSelectionModel().getSelectedItem();
        if (line == null || sale == null) {
            error.show("Sélectionnez une ligne.");
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(discount.getText().trim().replace(',', '.'));
        } catch (Exception e) {
            error.show("Remise invalide.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.lineDiscount(sale.id(), line.id(), amount), this::showSale, this::fail);
    }

    private void pay() {
        if (sale == null || sale.total() == null) {
            return;
        }
        String method = payMethod.getValue() == null ? "CASH" : payMethod.getValue();
        BigDecimal cash = null;
        if (cashReceived.getText() != null && !cashReceived.getText().isBlank()) {
            try {
                cash = new BigDecimal(cashReceived.getText().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                error.show("Montant reçu invalide.");
                return;
            }
        }
        loading.setLoading(true);
        BigDecimal amount = sale.total();
        BigDecimal received = cash;
        FxAsync.run(() -> pos.validate(sale.id(), method, amount, received), paid -> {
            error.hide();
            showSale(paid);
            javafx.scene.control.Alert done = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Vente " + (paid.saleNumber() == null ? "" : paid.saleNumber()) + " validée — "
                            + ProductLabels.price(paid.total()));
            done.setHeaderText("Encaissement");
            done.showAndWait();
            ensureSale();
        }, this::fail);
    }

    private void showTicket() {
        if (sale == null) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.ticket(sale.id()), node -> {
            loading.setLoading(false);
            String text = node == null ? "" : node.toPrettyString();
            javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(text);
            area.setEditable(false);
            area.setPrefSize(480, 360);
            javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Ticket");
            dialog.getDialogPane().setContent(area);
            dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
            dialog.showAndWait();
        }, this::fail);
    }

    private void showSale(Sale next) {
        loading.setLoading(false);
        this.sale = next;
        List<SaleLine> lines = next.lignes() == null ? List.of() : next.lignes();
        cart.getItems().setAll(lines);
        total.setText(next.total() == null ? "—" : ProductLabels.price(next.total()));
        if (Boolean.TRUE.equals(next.hasStockIssues())) {
            error.show("Stock insuffisant sur au moins une ligne.");
        } else {
            error.hide();
        }
        cashReceived.setText(next.total() == null ? "" : next.total().toPlainString());
    }

    private void fail(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api && api.isUnauthorized()) {
            return;
        }
        if (t instanceof ApiException api) {
            error.show(ApiException.userMessage(api));
        } else {
            error.show("Une erreur est survenue.");
        }
    }

    private static TableColumn<SaleLine, String> col(String title, java.util.function.Function<SaleLine, String> fn) {
        TableColumn<SaleLine, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        col.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        return col;
    }

    public void focusSearch() {
        search.requestFocus();
    }
}
