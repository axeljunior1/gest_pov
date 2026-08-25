package com.gestpov.desktop.ui.pos;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.Customer;
import com.gestpov.desktop.model.PosProduct;
import com.gestpov.desktop.model.Sale;
import com.gestpov.desktop.model.SaleLine;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.PosClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.Reloadable;
import com.gestpov.desktop.ui.component.ConfirmationDialog;
import com.gestpov.desktop.ui.component.EmptyState;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * POS Desktop aligné web : mode vendeur encaisse OU caisse centrale (préparation + encaissement).
 */
public final class PosView extends StackPane implements Reloadable {

    private static final String MODE_SELLER = "SELLER_COLLECTS_PAYMENT";
    private static final String MODE_CENTRAL = "CENTRAL_CASHIER";
    private static final String TYPE_CASHIER = "CASHIER";
    private static final String TYPE_SALES = "SALES";

    private final SessionContext session;
    private final PosClient pos;
    private final boolean canPrepare;
    private final boolean canCollect;
    private final boolean canOpenSession;
    private final boolean canCloseSession;

    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();

    private final ToggleButton tabSales = new ToggleButton("Préparation ventes");
    private final ToggleButton tabCashier = new ToggleButton("Encaissement");
    private final HBox stationBar = new HBox(8);

    private final Label modeBadge = new Label();
    private final Label stationBadge = new Label();
    private final TextField openingCash = new TextField("0");
    private final Button openSessionBtn = new Button("Ouvrir la session");
    private final Button closeSessionBtn = new Button("Fermer la session");
    private final VBox sessionOpenBar = new VBox(8);
    private final HBox sessionActiveBar = new HBox(10);
    private final Label activeLabel = new Label();
    private final VBox wrongSessionBar = new VBox(8);

    private final VBox salesWorkspace = new VBox(12);
    private final VBox cashierWorkspace = new VBox(12);
    private final StackPane body = new StackPane();

    private final TextField search = new TextField();
    private final ListView<PosProduct> results = new ListView<>();
    private final TableView<SaleLine> cart = new TableView<>();
    private final Label total = new Label(ProductLabels.price(BigDecimal.ZERO));
    private final Label changeLabel = new Label("");
    private final Label customerLabel = new Label("Aucun client");
    private final TextField customerSearch = new TextField();
    private final ListView<Customer> customerResults = new ListView<>();
    private final VBox quickCreateCustomerBox = new VBox(6);
    private final TextField newCustomerLastName = new TextField();
    private final TextField newCustomerFirstName = new TextField();
    private final TextField newCustomerPhone = new TextField();
    private final Button chooseCustomerBtn = new Button("Choisir un client…");
    private final Button detachCustomerBtn = new Button("Retirer le client");
    private javafx.scene.control.Dialog<Void> customerSearchDialog;
    private final TextField loyaltyPoints = new TextField();
    private final Button redeemLoyaltyBtn = new Button("Points fidélité");
    private final TextField qty = new TextField("1");
    private final TextField discount = new TextField("0");
    private final ComboBox<String> payMethod = new ComboBox<>();
    private final TextField cashReceived = new TextField();
    private final Button payBtn = new Button("Encaisser");
    private final Button splitPayBtn = new Button("Paiement fractionné…");
    private final Button sendBtn = new Button("Envoyer à la caisse");
    private final Button holdBtn = new Button("Mettre en attente");
    private final Button resumeBtn = new Button("Reprendre attente");
    private final HBox payRow = new HBox(10);
    private final Label paySectionLabel = new Label();

    private final ComboBox<String> pendingPayMethod = new ComboBox<>();
    private final TextField pendingCashReceived = new TextField();
    private final Button pendingSplitPayBtn = new Button("Paiement fractionné…");
    private final Label pendingChangeLabel = new Label("");
    private final TableView<Sale> pendingTable = new TableView<>();
    private final Label pendingHint = new Label();

    private final VBox recentSalesPanel = new VBox(8);
    private final TableView<Sale> recentSalesTable = new TableView<>();

    private Sale sale;
    private String salesFlowMode = MODE_SELLER;
    private String sessionType;
    /** Poste actif : SALES ou CASHIER (en mode unifié = CASHIER). */
    private String station = TYPE_CASHIER;

    public PosView(SessionContext session) {
        this.session = session;
        this.pos = new PosClient(session.api());
        this.canPrepare = session.hasPermission("pos.sale.create")
                || session.hasPermission("pos.sale.prepare")
                || session.hasPermission("pos.sale.send_to_payment");
        this.canCollect = session.hasPermission("pos.payment.collect")
                || session.hasPermission("pos.sale.validate")
                || session.hasPermission("pos.payment.validate");
        this.canOpenSession = session.hasPermission("pos.session.open")
                || canPrepare
                || canCollect;
        this.canCloseSession = session.hasPermission("pos.session.close")
                || canPrepare
                || canCollect;
        getChildren().addAll(build(), loading);
        boot();
    }

    private VBox build() {
        Label title = new Label("Caisse POS");
        title.getStyleClass().addAll("page-title", "pos-title");
        Label sub = new Label("F2 recherche · Entrée ajouter · Double-clic résultat · "
                + "F5/F6/F7 moyen de paiement · +/- quantité (ligne panier sélectionnée)");
        sub.getStyleClass().add("page-sub");

        modeBadge.getStyleClass().add("page-sub");
        modeBadge.setWrapText(true);

        ToggleGroup stations = new ToggleGroup();
        tabSales.setToggleGroup(stations);
        tabCashier.setToggleGroup(stations);
        tabSales.getStyleClass().add("button-secondary");
        tabCashier.getStyleClass().add("button-secondary");
        tabSales.setOnAction(e -> {
            if (!tabSales.isSelected()) {
                tabSales.setSelected(true);
            }
            switchStation(TYPE_SALES);
        });
        tabCashier.setOnAction(e -> {
            if (!tabCashier.isSelected()) {
                tabCashier.setSelected(true);
            }
            switchStation(TYPE_CASHIER);
        });
        stationBar.getChildren().addAll(tabSales, tabCashier);
        stationBar.setAlignment(Pos.CENTER_LEFT);

        openingCash.getStyleClass().add("pos-input");
        openingCash.setPromptText("Fond de caisse");
        openSessionBtn.getStyleClass().addAll("button-primary", "pos-action");
        openSessionBtn.setOnAction(e -> open());
        openSessionBtn.setDisable(!canOpenSession);
        sessionOpenBar.getChildren().add(new HBox(12,
                labelSection("Ouverture"), openingCash, openSessionBtn));
        sessionOpenBar.getStyleClass().addAll("card", "pos-session-card");
        HBox.setHgrow(openingCash, Priority.ALWAYS);

        closeSessionBtn.setText("Clôturer la session");
        closeSessionBtn.getStyleClass().addAll("button-danger", "pos-action");
        closeSessionBtn.setOnAction(e -> close());
        closeSessionBtn.setVisible(canCloseSession);
        closeSessionBtn.setManaged(canCloseSession);
        closeSessionBtn.setMinWidth(160);
        activeLabel.getStyleClass().add("pos-section-label");
        sessionActiveBar.getChildren().addAll(activeLabel, closeSessionBtn);
        sessionActiveBar.setAlignment(Pos.CENTER_LEFT);
        sessionActiveBar.getStyleClass().addAll("card", "pos-session-card");
        sessionActiveBar.setVisible(false);
        sessionActiveBar.setManaged(false);

        Label wrongTitle = new Label("Session incompatible avec ce poste");
        wrongTitle.getStyleClass().add("pos-section-label");
        Label wrongHint = new Label("Fermez la session ouverte, ou basculez de poste.");
        wrongHint.getStyleClass().add("page-sub");
        wrongHint.setWrapText(true);
        Button closeWrong = new Button("Fermer la session actuelle");
        closeWrong.getStyleClass().addAll("button-danger", "pos-action");
        closeWrong.setOnAction(e -> close());
        wrongSessionBar.getChildren().addAll(wrongTitle, wrongHint, closeWrong);
        wrongSessionBar.getStyleClass().addAll("card", "pos-session-card");
        wrongSessionBar.setVisible(false);
        wrongSessionBar.setManaged(false);

        buildSalesWorkspace();
        buildCashierWorkspace();
        buildRecentSalesPanel();
        body.getChildren().addAll(salesWorkspace, cashierWorkspace);

        VBox page = new VBox(14, title, sub, modeBadge, stationBadge, stationBar, error,
                sessionOpenBar, sessionActiveBar, wrongSessionBar, body, recentSalesPanel);
        page.getStyleClass().addAll("content", "pos-page");
        VBox.setVgrow(body, Priority.ALWAYS);
        page.setPadding(new Insets(0));

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F2 && salesWorkspace.isVisible()) {
                search.requestFocus();
                return;
            }
            String payKeyMethod = methodForKey(e.getCode());
            if (payKeyMethod != null) {
                if (salesWorkspace.isVisible() && payBtn.isVisible()) {
                    payMethod.getSelectionModel().select(payKeyMethod);
                    e.consume();
                } else if (cashierWorkspace.isVisible()) {
                    pendingPayMethod.getSelectionModel().select(payKeyMethod);
                    e.consume();
                }
            }
        });

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("page-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox wrap = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrap;
    }

    private void buildSalesWorkspace() {
        search.getStyleClass().add("pos-search");
        search.setPromptText("Nom, SKU ou code-barres… (Entrée = scan → panier)");
        search.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                onSearchOrScan();
            }
        });
        Button searchBtn = new Button("Chercher");
        searchBtn.getStyleClass().addAll("button-secondary", "pos-action");
        searchBtn.setOnAction(e -> onSearchOrScan());
        HBox searchBar = new HBox(10, search, searchBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        results.getStyleClass().add("pos-results");
        results.setPrefHeight(220);
        results.setPlaceholder(new EmptyState("Catalogue vide — créez un produit Actif ou scannez un code-barres"));
        results.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PosProduct item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String price = ProductLabels.price(item.unitPrice());
                String sku = item.sku() == null || item.sku().isBlank() ? "" : " · " + item.sku();
                String stock = item.stockAvailable() == null ? ""
                        : " · stock " + item.stockAvailable().stripTrailingZeros().toPlainString();
                setText(item.nom() + sku + stock + "  —  " + price);
            }
        });
        results.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                addSelected();
            }
        });

        qty.getStyleClass().add("pos-qty");
        qty.setPrefWidth(80);
        discount.getStyleClass().add("pos-input");
        discount.setPrefWidth(90);

        Button setQty = new Button("Qté");
        setQty.getStyleClass().addAll("button-secondary", "pos-action-sm");
        setQty.setOnAction(e -> changeQty());
        Button removeLine = new Button("Retirer");
        removeLine.getStyleClass().addAll("button-ghost", "pos-action-sm");
        removeLine.setOnAction(e -> removeSelectedLine());
        Button applyDisc = new Button("Remise");
        applyDisc.getStyleClass().addAll("button-secondary", "pos-action-sm");
        applyDisc.setOnAction(e -> applyDiscount());
        applyDisc.setVisible(session.hasPermission("pos.sale.discount"));
        applyDisc.setManaged(applyDisc.isVisible());

        cart.getStyleClass().add("pos-cart");
        cart.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cart.getColumns().addAll(
                col("Produit", SaleLine::productNom),
                col("Qté", l -> l.quantityInput() == null ? "" : l.quantityInput().stripTrailingZeros().toPlainString()),
                col("Prix", l -> ProductLabels.price(l.unitPrice())),
                col("Total", l -> ProductLabels.price(l.lineTotal()))
        );
        cart.setPlaceholder(new EmptyState("Panier vide"));
        cart.setItems(FXCollections.observableArrayList());
        cart.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.ADD) {
                bumpSelectedQty(BigDecimal.ONE);
                e.consume();
            } else if (e.getCode() == KeyCode.MINUS || e.getCode() == KeyCode.SUBTRACT) {
                bumpSelectedQty(BigDecimal.ONE.negate());
                e.consume();
            }
        });

        customerSearch.setPromptText("Nom, téléphone…");
        customerSearch.setOnAction(e -> searchCustomers());
        javafx.animation.PauseTransition customerSearchDebounce =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
        customerSearchDebounce.setOnFinished(e -> searchCustomers());
        customerSearch.textProperty().addListener((o, a, b) -> {
            customerSearchDebounce.stop();
            customerSearchDebounce.playFromStart();
        });

        customerResults.setPlaceholder(new EmptyState("Aucun client"));
        customerResults.getStyleClass().add("pos-panel");
        customerResults.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label name = new Label(item.displayName().isBlank() ? item.displayLabel() : item.displayName());
                name.setStyle("-fx-font-weight: 700;");
                String phoneText = item.phone() == null ? "" : item.phone();
                VBox box;
                if (phoneText.isBlank()) {
                    box = new VBox(name);
                } else {
                    Label phone = new Label(phoneText);
                    phone.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
                    box = new VBox(2, name, phone);
                }
                setText(null);
                setGraphic(box);
            }
        });
        customerResults.setOnMouseClicked(e -> {
            Customer selected = customerResults.getSelectionModel().getSelectedItem();
            if (selected != null) {
                attachCustomer(selected);
            }
        });
        customerResults.getItems().addListener((javafx.collections.ListChangeListener<Customer>) change -> {
            int count = customerResults.getItems().size();
            customerResults.setPrefHeight(Math.min(Math.max(count, 1), 6) * 44 + 4);
        });

        newCustomerLastName.setPromptText("Nom *");
        newCustomerFirstName.setPromptText("Prénom");
        newCustomerPhone.setPromptText("Téléphone");
        Button quickCreateBtn = new Button("Créer et associer");
        quickCreateBtn.getStyleClass().addAll("button-primary", "pos-action-sm");
        quickCreateBtn.setOnAction(e -> quickCreateCustomer());
        Label quickCreateHint = new Label("Aucun client trouvé — création rapide");
        quickCreateHint.getStyleClass().add("section-hint");
        VBox quickCreateFields = new VBox(6, newCustomerLastName, newCustomerFirstName, newCustomerPhone, quickCreateBtn);
        quickCreateCustomerBox.getChildren().setAll(quickCreateHint, quickCreateFields);
        quickCreateCustomerBox.getStyleClass().add("card");
        quickCreateCustomerBox.setPadding(new Insets(10));
        quickCreateCustomerBox.setVisible(false);
        quickCreateCustomerBox.setManaged(false);

        chooseCustomerBtn.getStyleClass().addAll("button-secondary", "pos-action-sm");
        chooseCustomerBtn.setOnAction(e -> openCustomerSearchDialog());
        detachCustomerBtn.getStyleClass().addAll("button-danger", "pos-action-sm");
        detachCustomerBtn.setOnAction(e -> detachCustomer());
        detachCustomerBtn.setVisible(false);
        detachCustomerBtn.setManaged(false);

        customerLabel.getStyleClass().add("pos-section-label");
        HBox customerHeaderRow = new HBox(10, customerLabel, chooseCustomerBtn, detachCustomerBtn);
        customerHeaderRow.setAlignment(Pos.CENTER_LEFT);

        loyaltyPoints.setPromptText("Points à utiliser");
        loyaltyPoints.setPrefWidth(100);
        redeemLoyaltyBtn.getStyleClass().addAll("button-secondary", "pos-action-sm");
        redeemLoyaltyBtn.setOnAction(e -> redeemLoyalty());
        redeemLoyaltyBtn.setVisible(false);
        redeemLoyaltyBtn.setManaged(false);
        loyaltyPoints.setVisible(false);
        loyaltyPoints.setManaged(false);
        HBox loyaltyRow = new HBox(8, loyaltyPoints, redeemLoyaltyBtn);
        loyaltyRow.setAlignment(Pos.CENTER_LEFT);

        boolean canCustomer = session.hasPermission("customer.read");
        VBox customerBox = new VBox(6, customerHeaderRow, loyaltyRow);
        customerBox.setVisible(canCustomer);
        customerBox.setManaged(canCustomer);

        payMethod.getItems().addAll("CASH", "CARD", "MOBILE_MONEY");
        payMethod.setConverter(methodConverter());
        payMethod.getSelectionModel().select("CASH");
        payMethod.valueProperty().addListener((o, a, b) -> updateChange());
        cashReceived.setPromptText("Reçu client");
        cashReceived.textProperty().addListener((o, a, b) -> updateChange());
        changeLabel.getStyleClass().add("pos-change");

        payBtn.getStyleClass().addAll("button-pay", "pos-pay-btn");
        payBtn.setOnAction(e -> pay());
        splitPayBtn.getStyleClass().addAll("button-secondary", "pos-action");
        splitPayBtn.setOnAction(e -> openSplitPaymentDialog(false));
        sendBtn.getStyleClass().addAll("button-primary", "pos-pay-btn");
        sendBtn.setOnAction(e -> sendToCash());
        holdBtn.getStyleClass().addAll("button-secondary", "pos-action");
        holdBtn.setOnAction(e -> holdCurrent());
        resumeBtn.getStyleClass().addAll("button-secondary", "pos-action");
        resumeBtn.setOnAction(e -> resumeHold());
        Button ticket = new Button("Ticket");
        ticket.getStyleClass().addAll("button-secondary", "pos-action-sm");
        ticket.setOnAction(e -> showTicket());

        Label totalCaption = new Label("TOTAL");
        totalCaption.getStyleClass().add("pos-total-caption");
        total.getStyleClass().add("pos-total-amount");
        HBox totalBar = new HBox(16, totalCaption, total);
        totalBar.getStyleClass().add("pos-total-bar");

        paySectionLabel.getStyleClass().add("pos-section-label");
        payRow.setAlignment(Pos.CENTER_LEFT);
        payRow.setSpacing(10);
        payRow.getChildren().addAll(payMethod, cashReceived, payBtn, splitPayBtn, sendBtn, holdBtn, resumeBtn, ticket);
        HBox.setHgrow(cashReceived, Priority.ALWAYS);

        HBox lineActions = new HBox(10, qty, setQty, removeLine, discount, applyDisc);
        lineActions.setAlignment(Pos.CENTER_LEFT);

        VBox searchCard = new VBox(10, labelSection("Catalogue"), searchBar, results);
        searchCard.getStyleClass().addAll("card", "pos-panel");
        VBox.setVgrow(results, Priority.ALWAYS);

        VBox cartCard = new VBox(12, labelSection("Panier"), cart, lineActions, customerBox,
                totalBar, paySectionLabel, payRow, changeLabel);
        cartCard.getStyleClass().addAll("card", "pos-panel", "pos-cart-panel");
        VBox.setVgrow(cart, Priority.ALWAYS);

        HBox workspace = new HBox(16, searchCard, cartCard);
        HBox.setHgrow(searchCard, Priority.ALWAYS);
        HBox.setHgrow(cartCard, Priority.ALWAYS);
        salesWorkspace.getChildren().setAll(workspace);
        VBox.setVgrow(workspace, Priority.ALWAYS);
    }

    private void buildCashierWorkspace() {
        pendingHint.getStyleClass().add("page-sub");
        pendingHint.setWrapText(true);
        pendingHint.setText("Ventes envoyées par les vendeurs — sélectionnez puis encaissez.");

        pendingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        pendingTable.getColumns().addAll(
                saleCol("N°", Sale::saleNumber),
                saleCol("Vendeur", s -> s.sellerName() == null ? "—" : s.sellerName()),
                saleCol("Client", s -> s.customerName() == null ? "—" : s.customerName()),
                saleCol("Total", s -> ProductLabels.price(s.total() == null ? BigDecimal.ZERO : s.total()))
        );
        pendingTable.setPlaceholder(new EmptyState("Aucune vente en attente de paiement"));
        pendingTable.setPrefHeight(280);

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().addAll("button-secondary", "pos-action");
        refresh.setOnAction(e -> reloadPending());

        Button encaisse = new Button("Encaisser la sélection");
        encaisse.getStyleClass().addAll("button-pay", "pos-pay-btn");
        encaisse.setOnAction(e -> payPendingSelected());

        Button recall = new Button("Retour vendeur");
        recall.getStyleClass().addAll("button-ghost", "pos-action");
        recall.setOnAction(e -> recallPendingSelected());

        pendingSplitPayBtn.getStyleClass().addAll("button-secondary", "pos-action");
        pendingSplitPayBtn.setOnAction(e -> openPendingSplitPayment());

        pendingPayMethod.getItems().addAll("CASH", "CARD", "MOBILE_MONEY");
        pendingPayMethod.setConverter(methodConverter());
        pendingPayMethod.getSelectionModel().select("CASH");
        pendingPayMethod.valueProperty().addListener((o, a, b) -> updatePendingChange());
        pendingCashReceived.setPromptText("Reçu client");
        pendingCashReceived.textProperty().addListener((o, a, b) -> updatePendingChange());
        pendingChangeLabel.getStyleClass().add("pos-change");

        HBox actions = new HBox(10, refresh, encaisse, pendingSplitPayBtn, recall);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox payPendingRow = new HBox(10, pendingPayMethod, pendingCashReceived);
        payPendingRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pendingCashReceived, Priority.ALWAYS);

        cashierWorkspace.getChildren().setAll(pendingHint, pendingTable, actions,
                labelSection("Paiement (après sélection → Encaisser)"),
                payPendingRow, pendingChangeLabel);
        VBox.setVgrow(pendingTable, Priority.ALWAYS);
        cashierWorkspace.getStyleClass().addAll("card", "pos-panel");
        cashierWorkspace.setPadding(new Insets(12));
    }

    private void buildRecentSalesPanel() {
        Label recentTitle = new Label("Ventes récentes (tous postes) — réimpression ticket");
        recentTitle.getStyleClass().add("pos-section-label");

        recentSalesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        recentSalesTable.getColumns().addAll(
                saleCol("N°", Sale::saleNumber),
                saleCol("Client", s -> s.customerName() == null ? "—" : s.customerName()),
                saleCol("Total", s -> ProductLabels.price(s.total() == null ? BigDecimal.ZERO : s.total()))
        );
        TableColumn<Sale, Void> ticketCol = new TableColumn<>("Ticket");
        ticketCol.setCellFactory(c -> new TableCell<>() {
            private final Button reprintBtn = new Button("Réimprimer");

            {
                reprintBtn.getStyleClass().addAll("button-ghost", "pos-action-sm");
                reprintBtn.setOnAction(e -> {
                    Sale s = getTableRow() == null ? null : getTableRow().getItem();
                    if (s != null && s.id() != null) {
                        offerTicketPrint(s.id());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : reprintBtn);
            }
        });
        recentSalesTable.getColumns().add(ticketCol);
        recentSalesTable.setPlaceholder(new EmptyState("Aucune vente récente"));
        recentSalesTable.setPrefHeight(180);

        Button refreshRecent = new Button("Actualiser");
        refreshRecent.getStyleClass().add("button-secondary");
        refreshRecent.setOnAction(e -> reloadRecentSales());

        recentSalesPanel.getChildren().setAll(new HBox(10, recentTitle, refreshRecent), recentSalesTable);
        recentSalesPanel.getStyleClass().addAll("card", "pos-panel");
        recentSalesPanel.setPadding(new Insets(12));
    }

    private void reloadRecentSales() {
        if (!canCollect) {
            return;
        }
        // sessionOnly=false : on veut les encaissements de tous les postes/utilisateurs, pas que ceux de ce PC
        FxAsync.run(() -> pos.listCompletedSales(false, 20), list -> recentSalesTable.getItems().setAll(list),
                ignored -> {
                    // panneau optionnel : on ignore les echecs silencieusement
                });
    }

    private void offerTicketPrint(long saleId) {
        FxAsync.run(() -> pos.ticket(saleId),
                node -> PosTicketHelper.showAndOfferPrint(getScene() == null ? null : getScene().getWindow(), node),
                t -> {
                    // impression optionnelle : on ne bloque pas le flux de vente si le ticket ne charge pas
                });
    }

    @Override
    public void reload() {
        loadCatalogPreview();
        if (sessionMatchesStation() && TYPE_CASHIER.equals(requiredType()) && canCollect && isCentral()) {
            reloadPending();
        }
        FxAsync.run(pos::context, ctx -> applyContext(ctx, false), this::fail);
    }

    private void boot() {
        loading.setLoading(true);
        FxAsync.run(pos::context, ctx -> {
            loading.setLoading(false);
            applyContext(ctx, true);
        }, this::fail);
    }

    private void applyContext(JsonNode ctx, boolean initStation) {
        salesFlowMode = readSalesFlowMode(ctx);
        boolean hasSession = ctx != null && ctx.hasNonNull("session") && !ctx.get("session").isNull();
        sessionType = hasSession ? textOrNull(ctx.get("session"), "sessionType") : null;

        if (initStation) {
            station = defaultStation();
        }
        syncStationTabs();
        refreshChrome();
        if (sessionMatchesStation() && TYPE_SALES.equals(requiredType()) && canPrepare) {
            loadCatalogPreview();
        }
        if (sessionMatchesStation() && TYPE_CASHIER.equals(requiredType()) && canCollect && isCentral()) {
            reloadPending();
        }
        if (sessionMatchesStation() && TYPE_CASHIER.equals(requiredType()) && !isCentral() && canPrepare) {
            loadCatalogPreview();
        }
    }

    private String defaultStation() {
        if (!isCentral()) {
            return TYPE_CASHIER;
        }
        if (canPrepare && !canCollect) {
            return TYPE_SALES;
        }
        if (canCollect && !canPrepare) {
            return TYPE_CASHIER;
        }
        if (TYPE_CASHIER.equals(sessionType)) {
            return TYPE_CASHIER;
        }
        return TYPE_SALES;
    }

    private void switchStation(String next) {
        station = next;
        syncStationTabs();
        refreshChrome();
        if (sessionMatchesStation() && TYPE_SALES.equals(requiredType())) {
            loadCatalogPreview();
        }
        if (sessionMatchesStation() && TYPE_CASHIER.equals(requiredType()) && isCentral()) {
            reloadPending();
        }
    }

    private void syncStationTabs() {
        boolean dual = isCentral() && canPrepare && canCollect;
        stationBar.setVisible(dual);
        stationBar.setManaged(dual);
        tabSales.setSelected(TYPE_SALES.equals(station));
        tabCashier.setSelected(TYPE_CASHIER.equals(station));
        if (isCentral() && canPrepare && !canCollect) {
            station = TYPE_SALES;
        }
        if (isCentral() && canCollect && !canPrepare) {
            station = TYPE_CASHIER;
        }
        if (!isCentral()) {
            station = TYPE_CASHIER;
        }
    }

    private void refreshChrome() {
        boolean central = isCentral();
        modeBadge.setText(central
                ? "Mode caisse centrale — le vendeur prépare, le caissier encaisse."
                : "Mode vendeur encaisse — vente et paiement sur le même poste.");

        String required = requiredType();
        boolean open = sessionType != null;
        boolean match = sessionMatchesStation();
        boolean wrong = open && !match;

        sessionOpenBar.setVisible(!open);
        sessionOpenBar.setManaged(!open);
        sessionActiveBar.setVisible(open && match);
        sessionActiveBar.setManaged(open && match);
        wrongSessionBar.setVisible(wrong);
        wrongSessionBar.setManaged(wrong);

        openingCash.setVisible(TYPE_CASHIER.equals(required));
        openingCash.setManaged(TYPE_CASHIER.equals(required));
        openSessionBtn.setText(TYPE_SALES.equals(required) ? "Ouvrir session vente" : "Ouvrir session caisse");

        if (TYPE_SALES.equals(sessionType)) {
            activeLabel.setText("Session vente ouverte");
        } else if (TYPE_CASHIER.equals(sessionType)) {
            activeLabel.setText("Session caisse ouverte");
        } else {
            activeLabel.setText("Session ouverte");
        }

        boolean showSalesUi = open && match && (TYPE_SALES.equals(required) || !central);
        boolean showCashierPending = open && match && central && TYPE_CASHIER.equals(required);

        salesWorkspace.setVisible(showSalesUi && !wrong);
        salesWorkspace.setManaged(showSalesUi && !wrong);
        cashierWorkspace.setVisible(showCashierPending && !wrong);
        cashierWorkspace.setManaged(showCashierPending && !wrong);

        // Unified seller-collects: sales workspace with pay button
        boolean showPay = !central && canCollect;
        boolean showSend = central && canPrepare && TYPE_SALES.equals(required);
        boolean showHold = canPrepare && (showPay || showSend);
        payBtn.setVisible(showPay);
        payBtn.setManaged(showPay);
        sendBtn.setVisible(showSend);
        sendBtn.setManaged(showSend);
        holdBtn.setVisible(showHold);
        holdBtn.setManaged(showHold);
        resumeBtn.setVisible(showHold);
        resumeBtn.setManaged(showHold);
        payMethod.setVisible(showPay);
        payMethod.setManaged(showPay);
        cashReceived.setVisible(showPay);
        cashReceived.setManaged(showPay);
        splitPayBtn.setVisible(showPay);
        splitPayBtn.setManaged(showPay);
        paySectionLabel.setText(showSend ? "Envoi caisse / attente" : "Paiement / attente");
        paySectionLabel.setVisible(showPay || showSend || showHold);
        paySectionLabel.setManaged(showPay || showSend || showHold);

        String badgeBase = "-fx-font-weight: 800; -fx-font-size: 14px; -fx-padding: 6 14; -fx-background-radius: 6;";
        if (central) {
            boolean onSalesStation = TYPE_SALES.equals(required);
            stationBadge.setText(onSalesStation ? "🛒 PRÉPARATION DES VENTES" : "🧾 ENCAISSEMENT");
            stationBadge.setStyle(badgeBase + (onSalesStation
                    ? "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;"
                    : "-fx-background-color: #dcfce7; -fx-text-fill: #166534;"));
        } else {
            stationBadge.setText("🧾 VENTE & ENCAISSEMENT");
            stationBadge.setStyle(badgeBase + "-fx-background-color: #e0e7ff; -fx-text-fill: #3730a3;");
        }
        stationBadge.setVisible(open && match && !wrong);
        stationBadge.setManaged(open && match && !wrong);

        boolean showRecent = canCollect && open && match && !wrong;
        recentSalesPanel.setVisible(showRecent);
        recentSalesPanel.setManaged(showRecent);
        if (showRecent) {
            reloadRecentSales();
        }
    }

    private String requiredType() {
        if (!isCentral()) {
            return TYPE_CASHIER;
        }
        return TYPE_SALES.equals(station) ? TYPE_SALES : TYPE_CASHIER;
    }

    private boolean sessionMatchesStation() {
        return sessionType != null && sessionType.equals(requiredType());
    }

    private boolean isCentral() {
        return MODE_CENTRAL.equals(salesFlowMode);
    }

    private void open() {
        String type = requiredType();
        BigDecimal cash = BigDecimal.ZERO;
        if (TYPE_CASHIER.equals(type)) {
            try {
                cash = parseDecimal(openingCash.getText(), BigDecimal.ZERO);
            } catch (NumberFormatException e) {
                error.show("Fond de caisse invalide.");
                return;
            }
        }
        loading.setLoading(true);
        BigDecimal opening = cash;
        String openType = type;
        FxAsync.runVoid(() -> pos.openSession(opening, openType), () -> {
            sessionType = openType;
            refreshChrome();
            loading.setLoading(false);
            if (TYPE_SALES.equals(openType) || !isCentral()) {
                loadCatalogPreview();
            }
            if (TYPE_CASHIER.equals(openType) && isCentral()) {
                reloadPending();
            }
        }, this::fail);
    }

    private void close() {
        if (TYPE_CASHIER.equals(sessionType)) {
            javafx.stage.Window owner = getScene() == null ? null : getScene().getWindow();
            PosCloseSessionDialog.show(owner, pos, report -> afterClosed(), this::fail);
            return;
        }
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Fermer la session vente",
                "Clôturer la session vendeur ? Les brouillons seront annulés.")) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.closeSession(BigDecimal.ZERO, true, null, null, null, null), report -> {
            loading.setLoading(false);
            afterClosed();
            int count = report == null ? 0 : report.path("saleCount").asInt(0);
            error.hide();
            javafx.scene.control.Alert done = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Session vente fermée — " + count + " vente(s).");
            done.setHeaderText("Clôture");
            done.showAndWait();
        }, this::fail);
    }

    private void afterClosed() {
        sale = null;
        sessionType = null;
        cart.getItems().clear();
        pendingTable.getItems().clear();
        total.setText(ProductLabels.price(BigDecimal.ZERO));
        refreshChrome();
    }

    /**
     * Exécute {@code action} avec une vente courante, en la créant à la demande si besoin
     * (au premier ajout réel — pas à l'ouverture de session, pour éviter les ventes fantômes à 0).
     */
    private void withSale(java.util.function.Consumer<Sale> action) {
        if (!canPrepare) {
            error.show("Ouvrez la session avant d'ajouter un produit.");
            return;
        }
        if (sale != null) {
            action.accept(sale);
            return;
        }
        loading.setLoading(true);
        FxAsync.run(pos::createSale, created -> {
            sale = created;
            action.accept(created);
        }, this::fail);
    }

    /** Réinitialise le panier affiché sans provisionner de nouvelle vente côté serveur. */
    private void resetCartForNextCustomer() {
        sale = null;
        cart.getItems().clear();
        total.setText(ProductLabels.price(BigDecimal.ZERO));
        customerLabel.setText("Aucun client");
        changeLabel.setText("");
        cashReceived.clear();
        chooseCustomerBtn.setVisible(true);
        chooseCustomerBtn.setManaged(true);
        detachCustomerBtn.setVisible(false);
        detachCustomerBtn.setManaged(false);
        redeemLoyaltyBtn.setVisible(false);
        redeemLoyaltyBtn.setManaged(false);
        loyaltyPoints.setVisible(false);
        loyaltyPoints.setManaged(false);
    }

    private void sendToCash() {
        if (sale == null || sale.lignes() == null || sale.lignes().isEmpty()) {
            error.show("Le panier est vide.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.sendToPayment(sale.id()), sent -> {
            loading.setLoading(false);
            error.hide();
            javafx.scene.control.Alert done = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Vente " + (sent.saleNumber() == null ? "" : sent.saleNumber())
                            + " envoyée à la caisse.");
            done.setHeaderText("Envoi caisse");
            done.showAndWait();
            resetCartForNextCustomer();
        }, this::fail);
    }

    private void holdCurrent() {
        if (sale == null || sale.lignes() == null || sale.lignes().isEmpty()) {
            error.show("Le panier est vide — rien à mettre en attente.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.holdSale(sale.id(), "Pause client"), held -> {
            loading.setLoading(false);
            error.hide();
            javafx.scene.control.Alert done = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Vente " + (held.saleNumber() == null ? "" : held.saleNumber())
                            + " mise en attente.");
            done.setHeaderText("En attente");
            done.showAndWait();
            resetCartForNextCustomer();
        }, this::fail);
    }

    private void resumeHold() {
        loading.setLoading(true);
        FxAsync.run(pos::listHold, list -> {
            loading.setLoading(false);
            if (list.isEmpty()) {
                error.show("Aucune vente en attente.");
                return;
            }
            ListView<Sale> lv = new ListView<>();
            lv.getItems().setAll(list);
            lv.setCellFactory(v -> new ListCell<>() {
                @Override
                protected void updateItem(Sale item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText((item.saleNumber() == null ? "#" + item.id() : item.saleNumber())
                                + " — " + ProductLabels.price(item.total() == null ? BigDecimal.ZERO : item.total())
                                + (item.customerName() == null ? "" : " · " + item.customerName()));
                    }
                }
            });
            lv.setPrefHeight(220);
            lv.getSelectionModel().selectFirst();
            javafx.scene.control.Dialog<Sale> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Reprendre une vente");
            dialog.setHeaderText("Ventes en attente");
            dialog.getDialogPane().setContent(lv);
            dialog.getDialogPane().getButtonTypes().addAll(
                    javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);
            dialog.setResultConverter(btn -> {
                if (btn == javafx.scene.control.ButtonType.OK) {
                    return lv.getSelectionModel().getSelectedItem();
                }
                return null;
            });
            dialog.showAndWait().ifPresent(selected -> {
                if (selected == null || selected.id() == null) {
                    return;
                }
                loading.setLoading(true);
                FxAsync.run(() -> pos.resumeSale(selected.id()), this::showSale, this::fail);
            });
        }, this::fail);
    }

    private void reloadPending() {
        if (!canCollect) {
            return;
        }
        pendingHint.setText("Chargement des ventes en attente…");
        loading.setLoading(true);
        FxAsync.run(pos::listPendingPayments, list -> {
            loading.setLoading(false);
            error.hide();
            pendingTable.getItems().setAll(list);
            pendingHint.setText(list.size() + " vente(s) en attente de paiement.");
        }, t -> {
            loading.setLoading(false);
            pendingHint.setText("Échec du chargement — voir le message d'erreur.");
            fail(t);
        });
    }

    private void loadCatalogPreview() {
        if (!canPrepare) {
            return;
        }
        String q = search.getText() == null ? "" : search.getText().trim();
        if (!q.isEmpty()) {
            return;
        }
        FxAsync.run(() -> pos.browseCatalog(20), list -> {
            results.getItems().setAll(list);
            if (list.isEmpty()) {
                results.setPlaceholder(new EmptyState(
                        "Aucun produit Actif — passez le cycle de vie à « Actif » sur la fiche produit"));
            }
        }, ignored -> {
        });
    }

    /** Entrée / Chercher : code-barres → ajout panier ; sinon recherche catalogue. */
    private void onSearchOrScan() {
        String q = search.getText() == null ? "" : search.getText().trim();
        if (looksLikeBarcode(q)) {
            scanBarcodeToCart(q);
            return;
        }
        searchNow();
    }

    private void scanBarcodeToCart(String code) {
        BigDecimal quantity;
        try {
            quantity = parseDecimal(qty.getText(), BigDecimal.ONE);
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                error.show("Quantité invalide.");
                return;
            }
        } catch (NumberFormatException e) {
            error.show("Quantité invalide.");
            return;
        }
        BigDecimal qtyToAdd = quantity;
        withSale(currentSale -> {
            loading.setLoading(true);
            FxAsync.run(() -> pos.scanItem(currentSale.id(), code, qtyToAdd), paid -> {
                loading.setLoading(false);
                error.hide();
                showSale(paid);
                search.clear();
                qty.setText("1");
                loadCatalogPreview();
                search.requestFocus();
            }, t -> {
                loading.setLoading(false);
                fail(t);
                search.selectAll();
                search.requestFocus();
            });
        });
    }

    private void searchNow() {
        String q = search.getText() == null ? "" : search.getText().trim();
        loading.setLoading(true);
        FxAsync.run(() -> q.isEmpty() ? pos.browseCatalog(20) : pos.search(q, 20), list -> {
            loading.setLoading(false);
            error.hide();
            results.getItems().setAll(list);
            if (list.isEmpty()) {
                if (q.isEmpty()) {
                    results.setPlaceholder(new EmptyState(
                            "Aucun produit Actif — vérifiez statut / cycle de vie sur Produits"));
                } else {
                    error.show("Aucun résultat pour « " + q + " ».");
                }
            } else if (list.size() == 1 && !q.isEmpty()) {
                results.getSelectionModel().select(0);
                addSelected();
            }
        }, this::fail);
    }

    private static boolean looksLikeBarcode(String term) {
        if (term == null || term.isBlank()) {
            return false;
        }
        String t = term.trim();
        if (t.length() < 6) {
            return false;
        }
        for (int i = 0; i < t.length(); i++) {
            if (!Character.isDigit(t.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void payPendingSelected() {
        Sale selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            error.show("Sélectionnez une vente à encaisser.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.getSale(selected.id()), full -> {
            loading.setLoading(false);
            sale = full;
            if (full.total() != null && (pendingCashReceived.getText() == null || pendingCashReceived.getText().isBlank())) {
                pendingCashReceived.setText(full.total().toPlainString());
            }
            updatePendingChange();
            payWith(pendingPayMethod.getValue(), pendingCashReceived.getText(), true);
        }, this::fail);
    }

    private void recallPendingSelected() {
        Sale selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.id() == null) {
            error.show("Sélectionnez une vente.");
            return;
        }
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Retour vendeur",
                "Renvoyer cette vente au poste vendeur ?")) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.recallFromPayment(selected.id()), ignored -> {
            loading.setLoading(false);
            reloadPending();
        }, this::fail);
    }

    private void addSelected() {
        PosProduct product = results.getSelectionModel().getSelectedItem();
        if (product == null) {
            return;
        }
        BigDecimal quantity;
        try {
            quantity = parseDecimal(qty.getText(), BigDecimal.ONE);
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                error.show("Quantité invalide.");
                return;
            }
        } catch (NumberFormatException e) {
            error.show("Quantité invalide.");
            return;
        }
        Long variantId = product.matchedVariantId();
        Long packagingId = product.matchedPackagingId();
        if (product.needsVariantPick()) {
            javafx.scene.control.ChoiceDialog<PosProduct.PosVariant> dlg =
                    new javafx.scene.control.ChoiceDialog<>(product.variants().get(0), product.variants());
            dlg.setTitle("Variante");
            dlg.setHeaderText(product.nom());
            dlg.setContentText("Choisir la variante :");
            var picked = dlg.showAndWait();
            if (picked.isEmpty()) {
                return;
            }
            variantId = picked.get().id();
            if (picked.get().packagings() != null && picked.get().packagings().size() > 1) {
                packagingId = pickPackaging(picked.get().packagings());
                if (packagingId == null && picked.get().packagings().size() > 1) {
                    return;
                }
            }
        } else if (product.needsPackagingPick()) {
            packagingId = pickPackaging(product.packagings());
            if (packagingId == null) {
                return;
            }
        }
        Long v = variantId;
        Long p = packagingId;
        withSale(currentSale -> {
            loading.setLoading(true);
            FxAsync.run(() -> pos.addLine(currentSale.id(), product.id(), v, p, quantity), this::showSale, this::fail);
        });
    }

    private Long pickPackaging(List<PosProduct.PosPackaging> packs) {
        if (packs == null || packs.isEmpty()) {
            return null;
        }
        if (packs.size() == 1) {
            return packs.get(0).id();
        }
        javafx.scene.control.ChoiceDialog<PosProduct.PosPackaging> dlg =
                new javafx.scene.control.ChoiceDialog<>(packs.get(0), packs);
        dlg.setTitle("Conditionnement");
        dlg.setHeaderText("Choisir le conditionnement");
        return dlg.showAndWait().map(PosProduct.PosPackaging::id).orElse(null);
    }

    private void changeQty() {
        SaleLine line = cart.getSelectionModel().getSelectedItem();
        if (line == null || sale == null) {
            error.show("Sélectionnez une ligne.");
            return;
        }
        BigDecimal quantity;
        try {
            quantity = parseDecimal(qty.getText(), null);
        } catch (Exception e) {
            error.show("Quantité invalide.");
            return;
        }
        Long lineId = line.id();
        loading.setLoading(true);
        FxAsync.run(() -> pos.updateQty(sale.id(), lineId, quantity), updated -> {
            showSale(updated);
            reselectLine(lineId);
        }, this::fail);
    }

    private void bumpSelectedQty(BigDecimal delta) {
        SaleLine line = cart.getSelectionModel().getSelectedItem();
        if (line == null || sale == null) {
            error.show("Sélectionnez une ligne du panier.");
            return;
        }
        BigDecimal current = line.quantityInput() == null ? BigDecimal.ZERO : line.quantityInput();
        BigDecimal next = current.add(delta);
        if (next.compareTo(BigDecimal.ONE) < 0) {
            next = BigDecimal.ONE;
        }
        Long lineId = line.id();
        loading.setLoading(true);
        BigDecimal quantity = next;
        FxAsync.run(() -> pos.updateQty(sale.id(), lineId, quantity), updated -> {
            showSale(updated);
            reselectLine(lineId);
        }, this::fail);
    }

    /** Reconstitue la sélection/focus de la table après un refresh (les lignes sont de nouvelles instances). */
    private void reselectLine(Long lineId) {
        if (lineId == null) {
            return;
        }
        for (SaleLine l : cart.getItems()) {
            if (lineId.equals(l.id())) {
                cart.getSelectionModel().select(l);
                break;
            }
        }
        cart.requestFocus();
    }

    private void removeSelectedLine() {
        SaleLine line = cart.getSelectionModel().getSelectedItem();
        if (line == null || sale == null) {
            error.show("Sélectionnez une ligne à retirer.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.removeLine(sale.id(), line.id()), this::showSale, this::fail);
    }

    private void applyDiscount() {
        SaleLine line = cart.getSelectionModel().getSelectedItem();
        if (line == null || sale == null) {
            error.show("Sélectionnez une ligne.");
            return;
        }
        BigDecimal amount;
        try {
            amount = parseDecimal(discount.getText(), BigDecimal.ZERO);
        } catch (Exception e) {
            error.show("Remise invalide.");
            return;
        }
        submitDiscount(sale.id(), line.id(), amount, null, null, null, null);
    }

    private void submitDiscount(long saleId, long lineId, BigDecimal amount, String managerEmail,
                                String managerPassword, String managerBadgeCode, String managerPin) {
        loading.setLoading(true);
        FxAsync.run(() -> pos.lineDiscount(saleId, lineId, amount, managerEmail, managerPassword,
                managerBadgeCode, managerPin), updated -> {
            showSale(updated);
            reselectLine(lineId);
        }, t -> {
            loading.setLoading(false);
            if (t instanceof ApiException api && !api.isUnauthorized()
                    && ApiException.userMessage(api).contains("Validation manager obligatoire")) {
                promptDiscountManagerApproval(saleId, lineId, amount);
            } else {
                fail(t);
            }
        });
    }

    private void promptDiscountManagerApproval(long saleId, long lineId, BigDecimal amount) {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Validation manager requise");
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        Label info = new Label("Cette remise dépasse le seuil autorisé — validation d'un manager obligatoire.");
        info.setWrapText(true);
        info.getStyleClass().add("page-sub");
        javafx.scene.control.TextField managerEmail = new javafx.scene.control.TextField();
        managerEmail.setPromptText("Email manager");
        javafx.scene.control.PasswordField managerPassword = new javafx.scene.control.PasswordField();
        managerPassword.setPromptText("Mot de passe manager");
        javafx.scene.control.TextField managerBadge = new javafx.scene.control.TextField();
        managerBadge.setPromptText("Badge manager");
        javafx.scene.control.PasswordField managerPin = new javafx.scene.control.PasswordField();
        managerPin.setPromptText("Code PIN manager");
        Label dialogError = new Label();
        dialogError.getStyleClass().add("error-banner-text");
        dialogError.setWrapText(true);
        dialogError.setVisible(false);
        dialogError.setManaged(false);

        VBox emailBox = new VBox(8, labeled("Email", managerEmail), labeled("Mot de passe", managerPassword));
        VBox badgeBox = new VBox(8, labeled("Badge", managerBadge), labeled("Code PIN", managerPin));
        badgeBox.setVisible(false);
        badgeBox.setManaged(false);
        Button toggleMode = new Button("Utiliser un badge");
        toggleMode.getStyleClass().add("button-ghost");
        toggleMode.setOnAction(e -> {
            boolean toBadge = !badgeBox.isVisible();
            emailBox.setVisible(!toBadge);
            emailBox.setManaged(!toBadge);
            badgeBox.setVisible(toBadge);
            badgeBox.setManaged(toBadge);
            toggleMode.setText(toBadge ? "Utiliser email + mot de passe" : "Utiliser un badge");
        });

        Button confirm = new Button("Valider la remise");
        confirm.getStyleClass().add("button-primary");
        confirm.setOnAction(e -> {
            boolean hasEmailPwd = managerEmail.getText() != null && !managerEmail.getText().isBlank()
                    && managerPassword.getText() != null && !managerPassword.getText().isBlank();
            boolean hasBadge = managerBadge.getText() != null && !managerBadge.getText().isBlank()
                    && managerPin.getText() != null && !managerPin.getText().isBlank();
            if (!hasEmailPwd && !hasBadge) {
                dialogError.setText("Identifiants manager obligatoires (email+mot de passe ou badge+PIN).");
                dialogError.setVisible(true);
                dialogError.setManaged(true);
                return;
            }
            dialog.close();
            submitDiscount(saleId, lineId, amount,
                    hasEmailPwd ? managerEmail.getText().trim() : null,
                    hasEmailPwd ? managerPassword.getText() : null,
                    hasBadge ? managerBadge.getText().trim() : null,
                    hasBadge ? managerPin.getText() : null);
        });

        VBox content = new VBox(10, info, dialogError, emailBox, badgeBox, toggleMode, confirm);
        content.setPadding(new Insets(12));
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private static VBox labeled(String title, javafx.scene.Node node) {
        Label l = new Label(title);
        l.getStyleClass().add("form-label");
        return new VBox(4, l, node);
    }

    private void openCustomerSearchDialog() {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Choisir un client");
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        customerSearch.clear();
        customerResults.getItems().clear();
        quickCreateCustomerBox.setVisible(false);
        quickCreateCustomerBox.setManaged(false);

        Label title = new Label("Rechercher un client");
        title.getStyleClass().add("section-title");
        VBox content = new VBox(10, title, customerSearch, customerResults, quickCreateCustomerBox);
        content.setPadding(new Insets(12));
        content.setPrefWidth(380);
        dialog.getDialogPane().setContent(content);

        customerSearchDialog = dialog;
        javafx.application.Platform.runLater(customerSearch::requestFocus);
        searchCustomers();
        dialog.showAndWait();
        customerSearchDialog = null;
    }

    private void searchCustomers() {
        String q = customerSearch.getText() == null ? "" : customerSearch.getText().trim();
        loading.setLoading(true);
        FxAsync.run(() -> pos.searchCustomers(q, 20), list -> {
            loading.setLoading(false);
            error.hide();
            customerResults.getItems().setAll(list);
            boolean canCreateCustomer = session.hasPermission("customer.create");
            boolean showQuickCreate = list.isEmpty() && !q.isEmpty() && canCreateCustomer;
            if (showQuickCreate) {
                newCustomerLastName.setText(q);
                newCustomerFirstName.clear();
                newCustomerPhone.clear();
            }
            quickCreateCustomerBox.setVisible(showQuickCreate);
            quickCreateCustomerBox.setManaged(showQuickCreate);
        }, this::fail);
    }

    private void attachCustomer(Customer c) {
        if (c == null || c.id() == null) {
            return;
        }
        withSale(currentSale -> {
            loading.setLoading(true);
            FxAsync.run(() -> pos.assignCustomer(currentSale.id(), c.id()), updated -> {
                showSale(updated);
                if (customerSearchDialog != null) {
                    customerSearchDialog.close();
                }
            }, this::fail);
        });
    }

    private void quickCreateCustomer() {
        if (newCustomerLastName.getText() == null || newCustomerLastName.getText().isBlank()) {
            error.show("Nom obligatoire.");
            return;
        }
        String lastName = newCustomerLastName.getText().trim();
        String firstName = newCustomerFirstName.getText() == null ? "" : newCustomerFirstName.getText().trim();
        String phone = newCustomerPhone.getText() == null ? "" : newCustomerPhone.getText().trim();
        loading.setLoading(true);
        FxAsync.run(() -> pos.quickCreateCustomer(lastName, firstName, phone), created -> {
            loading.setLoading(false);
            error.hide();
            attachCustomer(created);
        }, t -> {
            loading.setLoading(false);
            fail(t);
        });
    }

    private void detachCustomer() {
        if (sale == null || !sale.hasCustomer()) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.clearCustomer(sale.id()), this::showSale, this::fail);
    }

    private void redeemLoyalty() {
        if (sale == null || !sale.hasCustomer()) {
            error.show("Associez un client avant d'utiliser des points.");
            return;
        }
        int points;
        try {
            points = Integer.parseInt(loyaltyPoints.getText().trim());
        } catch (Exception e) {
            error.show("Nombre de points invalide.");
            return;
        }
        if (points <= 0) {
            error.show("Saisissez un nombre de points positif.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.redeemLoyalty(sale.id(), points), this::showSale, this::fail);
    }

    private void pay() {
        payWith(payMethod.getValue(), cashReceived.getText(), false);
    }

    private void payWith(String methodRaw, String cashText, boolean fromPending) {
        if (sale == null || sale.total() == null) {
            return;
        }
        if (sale.lignes() == null || sale.lignes().isEmpty()) {
            error.show("Le panier est vide.");
            return;
        }
        String method = methodRaw == null ? "CASH" : methodRaw;
        BigDecimal cash = null;
        if (cashText != null && !cashText.isBlank()) {
            try {
                cash = parseDecimal(cashText, null);
            } catch (NumberFormatException e) {
                error.show("Montant reçu invalide.");
                return;
            }
        }
        if ("CASH".equals(method) && cash != null && cash.compareTo(sale.total()) < 0) {
            error.show("Montant reçu inférieur au total.");
            return;
        }
        loading.setLoading(true);
        BigDecimal amount = sale.total();
        BigDecimal received = cash;
        FxAsync.run(() -> pos.validate(sale.id(), method, amount, received), paid -> {
            error.hide();
            if (!fromPending) {
                showSale(paid);
            }
            String changeInfo = "";
            if ("CASH".equals(method) && received != null && paid.total() != null) {
                BigDecimal change = received.subtract(paid.total());
                if (change.compareTo(BigDecimal.ZERO) > 0) {
                    changeInfo = "\nMonnaie : " + ProductLabels.price(change);
                }
            }
            javafx.scene.control.Alert done = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Vente " + (paid.saleNumber() == null ? "" : paid.saleNumber()) + " validée — "
                            + ProductLabels.price(paid.total()) + changeInfo);
            done.setHeaderText("Encaissement");
            done.showAndWait();
            if (paid.id() != null) {
                offerTicketPrint(paid.id());
            }
            reloadRecentSales();
            if (fromPending) {
                sale = null;
                reloadPending();
            } else {
                resetCartForNextCustomer();
            }
        }, this::fail);
    }

    private void openPendingSplitPayment() {
        Sale selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            error.show("Sélectionnez une vente à encaisser.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.getSale(selected.id()), full -> {
            loading.setLoading(false);
            sale = full;
            openSplitPaymentDialog(true);
        }, this::fail);
    }

    /** Encaissement fractionné : plusieurs lignes {méthode, montant} devant sommer au total de la vente. */
    private void openSplitPaymentDialog(boolean fromPending) {
        if (sale == null || sale.total() == null) {
            error.show("Aucune vente à encaisser.");
            return;
        }
        if (sale.lignes() == null || sale.lignes().isEmpty()) {
            error.show("Le panier est vide.");
            return;
        }
        BigDecimal saleTotal = sale.total();

        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Paiement fractionné");
        dialog.setHeaderText("Total à encaisser : " + ProductLabels.price(saleTotal));
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        VBox linesBox = new VBox(8);
        Label remainingLabel = new Label();
        remainingLabel.getStyleClass().add("pos-change");
        Button addLineBtn = new Button("+ Ajouter un moyen de paiement");
        addLineBtn.getStyleClass().addAll("button-secondary", "pos-action-sm");
        Button confirmBtn = new Button("Valider le paiement");
        confirmBtn.getStyleClass().addAll("button-pay", "pos-pay-btn");

        record SplitRow(ComboBox<String> method, TextField amount) {
        }
        List<SplitRow> rows = new java.util.ArrayList<>();
        Runnable[] recompute = new Runnable[1];

        Runnable addRow = () -> {
            ComboBox<String> methodBox = new ComboBox<>();
            methodBox.getItems().addAll("CASH", "CARD", "MOBILE_MONEY");
            methodBox.setConverter(methodConverter());
            methodBox.getSelectionModel().select("CASH");
            TextField amountField = new TextField();
            amountField.setPromptText("Montant");
            amountField.setPrefWidth(120);
            Button removeBtn = new Button("×");
            removeBtn.getStyleClass().addAll("button-ghost", "pos-action-sm");
            HBox row = new HBox(8, methodBox, amountField, removeBtn);
            row.setAlignment(Pos.CENTER_LEFT);
            SplitRow splitRow = new SplitRow(methodBox, amountField);
            removeBtn.setOnAction(ev -> {
                rows.remove(splitRow);
                linesBox.getChildren().remove(row);
                recompute[0].run();
            });
            methodBox.valueProperty().addListener((o, a, b) -> recompute[0].run());
            amountField.textProperty().addListener((o, a, b) -> recompute[0].run());
            rows.add(splitRow);
            linesBox.getChildren().add(row);
            recompute[0].run();
        };

        recompute[0] = () -> {
            BigDecimal sum = BigDecimal.ZERO;
            for (SplitRow r : rows) {
                try {
                    sum = sum.add(parseDecimal(r.amount().getText(), BigDecimal.ZERO));
                } catch (Exception ignored) {
                    // montant partiellement saisi : ignoré dans le calcul en cours
                }
            }
            BigDecimal remaining = saleTotal.subtract(sum).setScale(2, RoundingMode.HALF_UP);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                remainingLabel.setText("Restant à payer : " + ProductLabels.price(remaining));
            } else if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remainingLabel.setText("Excédent : " + ProductLabels.price(remaining.abs()));
            } else {
                remainingLabel.setText("Montant complet.");
            }
            confirmBtn.setDisable(rows.isEmpty() || remaining.compareTo(BigDecimal.ZERO) != 0);
        };

        addLineBtn.setOnAction(ev -> addRow.run());
        addRow.run();
        rows.get(0).amount().setText(saleTotal.toPlainString());

        confirmBtn.setOnAction(ev -> {
            List<Map<String, Object>> payments = new java.util.ArrayList<>();
            BigDecimal cashAmount = null;
            for (SplitRow r : rows) {
                String method = r.method().getValue() == null ? "CASH" : r.method().getValue();
                BigDecimal amount;
                try {
                    amount = parseDecimal(r.amount().getText(), null);
                } catch (Exception ex) {
                    error.show("Montant invalide sur une ligne de paiement.");
                    return;
                }
                payments.add(java.util.Map.of("method", method, "amount", amount));
                if ("CASH".equals(method)) {
                    cashAmount = cashAmount == null ? amount : cashAmount.add(amount);
                }
            }
            BigDecimal received = cashAmount;
            loading.setLoading(true);
            FxAsync.run(() -> pos.validate(sale.id(), payments, received), paid -> {
                error.hide();
                dialog.close();
                if (!fromPending) {
                    showSale(paid);
                }
                javafx.scene.control.Alert done = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION,
                        "Vente " + (paid.saleNumber() == null ? "" : paid.saleNumber()) + " validée — "
                                + ProductLabels.price(paid.total()));
                done.setHeaderText("Encaissement fractionné");
                done.showAndWait();
                if (paid.id() != null) {
                    offerTicketPrint(paid.id());
                }
                reloadRecentSales();
                if (fromPending) {
                    sale = null;
                    reloadPending();
                } else {
                    resetCartForNextCustomer();
                }
            }, t -> {
                loading.setLoading(false);
                fail(t);
            });
        });

        VBox content = new VBox(10, linesBox, addLineBtn, remainingLabel, confirmBtn);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showTicket() {
        if (sale == null) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.ticket(sale.id()), node -> {
            loading.setLoading(false);
            PosTicketHelper.showAndOfferPrint(getScene() == null ? null : getScene().getWindow(), node);
        }, this::fail);
    }

    private void showSale(Sale next) {
        loading.setLoading(false);
        sale = next;
        cart.getItems().setAll(next.lignes() == null ? List.of() : next.lignes());
        total.setText(ProductLabels.price(next.total() == null ? BigDecimal.ZERO : next.total()));
        boolean hasCustomer = next.hasCustomer();
        if (hasCustomer) {
            customerLabel.setText("Client : " + (next.customerName() == null ? "#" + next.customerId() : next.customerName())
                    + (next.customerLoyaltyPoints() == null ? "" : " · " + next.customerLoyaltyPoints() + " pts"));
        } else {
            customerLabel.setText("Aucun client");
        }
        chooseCustomerBtn.setVisible(!hasCustomer);
        chooseCustomerBtn.setManaged(!hasCustomer);
        detachCustomerBtn.setVisible(hasCustomer);
        detachCustomerBtn.setManaged(hasCustomer);
        boolean canRedeem = hasCustomer && session.hasPermission("loyalty.redeem");
        redeemLoyaltyBtn.setVisible(canRedeem);
        redeemLoyaltyBtn.setManaged(canRedeem);
        loyaltyPoints.setVisible(canRedeem);
        loyaltyPoints.setManaged(canRedeem);
        if ("CASH".equals(payMethod.getValue()) && next.total() != null
                && (cashReceived.getText() == null || cashReceived.getText().isBlank()
                || cart.getItems().isEmpty())) {
            cashReceived.setText(next.total().toPlainString());
        }
        updateChange();
    }

    private void updateChange() {
        if (!payBtn.isVisible() || !"CASH".equals(payMethod.getValue()) || sale == null || sale.total() == null) {
            changeLabel.setText("");
            return;
        }
        try {
            BigDecimal received = parseDecimal(cashReceived.getText(), null);
            BigDecimal change = received.subtract(sale.total()).setScale(2, RoundingMode.HALF_UP);
            if (change.compareTo(BigDecimal.ZERO) >= 0) {
                changeLabel.setText("Monnaie à rendre : " + ProductLabels.price(change));
            } else {
                changeLabel.setText("Manque : " + ProductLabels.price(change.abs()));
            }
        } catch (Exception e) {
            changeLabel.setText("");
        }
    }

    private void updatePendingChange() {
        Sale selected = pendingTable.getSelectionModel().getSelectedItem();
        BigDecimal totalAmt = selected == null ? null : selected.total();
        if (sale != null && sale.total() != null) {
            totalAmt = sale.total();
        }
        if (!"CASH".equals(pendingPayMethod.getValue()) || totalAmt == null) {
            pendingChangeLabel.setText("");
            return;
        }
        try {
            BigDecimal received = parseDecimal(pendingCashReceived.getText(), null);
            BigDecimal change = received.subtract(totalAmt).setScale(2, RoundingMode.HALF_UP);
            if (change.compareTo(BigDecimal.ZERO) >= 0) {
                pendingChangeLabel.setText("Monnaie à rendre : " + ProductLabels.price(change));
            } else {
                pendingChangeLabel.setText("Manque : " + ProductLabels.price(change.abs()));
            }
        } catch (Exception e) {
            pendingChangeLabel.setText("");
        }
    }

    private void fail(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api && api.isUnauthorized()) {
            return;
        }
        String message;
        if (t instanceof ApiException api) {
            message = ApiException.userMessage(api);
        } else if (t != null && t.getMessage() != null && !t.getMessage().isBlank()) {
            message = t.getMessage();
        } else {
            message = "Une erreur est survenue.";
        }
        error.show(message);
    }

    public void focusSearch() {
        search.requestFocus();
    }

    private static String readSalesFlowMode(JsonNode ctx) {
        if (ctx == null) {
            return MODE_SELLER;
        }
        JsonNode cfg = ctx.get("posConfig");
        if (cfg != null && cfg.hasNonNull("salesFlowMode")) {
            return cfg.get("salesFlowMode").asText(MODE_SELLER);
        }
        if (cfg != null && cfg.hasNonNull("cashHandlingMode")) {
            return "CENTRAL_CASHIER".equals(cfg.get("cashHandlingMode").asText(""))
                    ? MODE_CENTRAL : MODE_SELLER;
        }
        return MODE_SELLER;
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String v = node.get(field).asText(null);
        return v == null || v.isBlank() ? null : v;
    }

    private static Label labelSection(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("pos-section-label");
        return l;
    }

    private static BigDecimal parseDecimal(String text, BigDecimal defaultValue) {
        if (text == null || text.isBlank()) {
            if (defaultValue == null) {
                throw new NumberFormatException("empty");
            }
            return defaultValue;
        }
        return new BigDecimal(text.trim().replace(',', '.'));
    }

    private static TableColumn<SaleLine, String> col(String title, java.util.function.Function<SaleLine, String> fn) {
        TableColumn<SaleLine, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static TableColumn<Sale, String> saleCol(String title, java.util.function.Function<Sale, String> fn) {
        TableColumn<Sale, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static String methodForKey(KeyCode code) {
        return switch (code) {
            case F5 -> "CASH";
            case F6 -> "CARD";
            case F7 -> "MOBILE_MONEY";
            default -> null;
        };
    }

    private static javafx.util.StringConverter<String> methodConverter() {
        return new javafx.util.StringConverter<>() {
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
        };
    }
}
