package com.gestpov.desktop.ui.pos;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.PosProduct;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.PosClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.Reloadable;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.ListPager;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.ui.products.ProductLabels;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PosReturnsView extends StackPane implements Reloadable {

    private final PosClient pos;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TextField search = new TextField();
    private final TableView<JsonNode> sales = new TableView<>();
    private final ListPager<JsonNode> salesPager = new ListPager<>(sales);
    private final TableView<LineRow> lines = new TableView<>();
    private final CheckBox selectAll = new CheckBox("Tout sélectionner");
    private final Label detail = new Label();
    private final ComboBox<String> payMethod = new ComboBox<>();
    private final TextField reason = new TextField();
    private JsonNode selectedReturnable;

    private final TextField exchangeSearch = new TextField();
    private final ComboBox<PosProduct> exchangeResults = new ComboBox<>();
    private final TextField exchangeQty = new TextField("1");
    private final TableView<ExchangeItemRow> exchangeItems = new TableView<>();
    private final Label exchangeSummary = new Label();
    private final ComboBox<String> exchangePayMethod = new ComboBox<>();

    public PosReturnsView(SessionContext session) {
        this.pos = new PosClient(session.api());
        getChildren().addAll(build(), loading);
        searchSales();
    }

    @Override
    public void reload() {
        error.hide();
        searchSales();
    }

    private VBox build() {
        Label title = new Label("Retours POS");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Ventes récentes remboursables — affinez avec un n° de vente ou un client si besoin");
        sub.getStyleClass().add("page-sub");

        search.setPromptText("N° vente ou client… (laisser vide = ventes récentes)");
        search.setOnAction(e -> searchSales());
        Button find = new Button("Chercher");
        find.getStyleClass().add("button-secondary");
        find.setOnAction(e -> searchSales());
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        sales.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        sales.getColumns().addAll(
                col("N°", n -> text(n, "saleNumber")),
                col("Client", n -> text(n, "customerName")),
                col("Total", n -> money(n, "total")),
                col("Remboursable", n -> money(n, "amountRefundable"))
        );
        sales.setPlaceholder(new EmptyState("Aucune vente remboursable"));
        sales.setPrefHeight(240);
        sales.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> loadReturnable(b));

        detail.getStyleClass().add("page-sub");
        detail.setWrapText(true);

        lines.setEditable(true);
        lines.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<LineRow, Boolean> selCol = new TableColumn<>("Rembourser");
        selCol.setCellValueFactory(d -> d.getValue().selected);
        selCol.setCellFactory(CheckBoxTableCell.forTableColumn(selCol));
        selCol.setEditable(true);
        selCol.setPrefWidth(90);

        TableColumn<LineRow, String> labelCol = new TableColumn<>("Produit");
        labelCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().label));

        TableColumn<LineRow, String> soldCol = new TableColumn<>("Vendu");
        soldCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(plain(d.getValue().quantitySold)));
        soldCol.setPrefWidth(80);

        TableColumn<LineRow, String> returnableCol = new TableColumn<>("Remboursable");
        returnableCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(plain(d.getValue().quantityReturnable)));
        returnableCol.setPrefWidth(100);

        TableColumn<LineRow, String> qtyCol = new TableColumn<>("Qté à rembourser");
        qtyCol.setCellValueFactory(d -> d.getValue().quantityText);
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn());
        qtyCol.setEditable(true);
        qtyCol.setOnEditCommit(e -> e.getRowValue().quantityText.set(e.getNewValue()));
        qtyCol.setPrefWidth(120);

        TableColumn<LineRow, Boolean> restockCol = new TableColumn<>("Remise en stock");
        restockCol.setCellValueFactory(d -> d.getValue().restock);
        restockCol.setCellFactory(CheckBoxTableCell.forTableColumn(restockCol));
        restockCol.setEditable(true);
        restockCol.setPrefWidth(110);

        TableColumn<LineRow, String> maxCol = new TableColumn<>("Montant max");
        maxCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(ProductLabels.price(d.getValue().maxRefundAmount)));

        lines.getColumns().addAll(selCol, labelCol, soldCol, returnableCol, qtyCol, restockCol, maxCol);
        lines.setPlaceholder(new EmptyState("Sélectionnez une vente remboursable ci-dessus"));
        lines.setPrefHeight(260);

        selectAll.setOnAction(e -> {
            boolean checked = selectAll.isSelected();
            for (LineRow row : lines.getItems()) {
                if (row.canReturn()) {
                    row.selected.set(checked);
                }
            }
        });

        reason.setPromptText("Motif du retour");
        payMethod.getItems().addAll("CASH", "CARD", "MOBILE_MONEY");
        payMethod.getSelectionModel().select("CASH");

        Button create = new Button("Créer le retour");
        create.getStyleClass().add("button-primary");
        create.setOnAction(e -> createAndValidate());

        HBox bar = new HBox(10, search, find, refresh);
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox actions = new HBox(10, reason, payMethod, create);
        HBox.setHgrow(reason, Priority.ALWAYS);

        VBox exchangeSection = buildExchangeSection();

        VBox scrollable = new VBox(12, sales, salesPager.bar(), detail, selectAll, lines, actions, exchangeSection);

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(scrollable);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("settings-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox page = new VBox(12, title, sub, error, bar, scroll);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    /** Échange : reprend les lignes cochées ci-dessus + donne un ou plusieurs articles en remplacement,
     * en une seule opération tracée (pas un retour puis une vente sans lien). */
    private VBox buildExchangeSection() {
        Label title = new Label("Échanger contre d'autres articles (optionnel)");
        title.getStyleClass().add("settings-group-title");
        Label hint = new Label("Cochez les lignes à reprendre ci-dessus, ajoutez les articles donnés en "
                + "remplacement, puis confirmez — un seul ticket, une seule opération.");
        hint.getStyleClass().add("page-sub");
        hint.setWrapText(true);

        exchangeSearch.setPromptText("Rechercher un article de remplacement…");
        exchangeQty.setPrefWidth(70);
        Button search2 = new Button("Chercher");
        search2.getStyleClass().add("button-secondary");
        search2.setOnAction(e -> searchExchangeProducts());
        exchangeSearch.setOnAction(e -> searchExchangeProducts());
        exchangeResults.setPromptText("Résultats…");
        exchangeResults.setMaxWidth(260);
        javafx.util.Callback<javafx.scene.control.ListView<PosProduct>, javafx.scene.control.ListCell<PosProduct>> exchangeCellFactory = lv -> new javafx.scene.control.ListCell<>() {
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
        };
        exchangeResults.setCellFactory(exchangeCellFactory);
        exchangeResults.setButtonCell(exchangeCellFactory.call(null));
        Button addItem = new Button("+ Ajouter");
        addItem.getStyleClass().addAll("button-secondary", "pos-action-sm");
        addItem.setOnAction(e -> addExchangeItem());

        HBox pickBar = new HBox(8, exchangeSearch, search2, exchangeResults, exchangeQty, addItem);
        HBox.setHgrow(exchangeSearch, Priority.ALWAYS);

        TableColumn<ExchangeItemRow, String> nameCol = new TableColumn<>("Article");
        nameCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().product.nom()));
        TableColumn<ExchangeItemRow, String> qtyCol2 = new TableColumn<>("Qté");
        qtyCol2.setCellValueFactory(d -> new ReadOnlyStringWrapper(plain(d.getValue().qty)));
        qtyCol2.setPrefWidth(70);
        TableColumn<ExchangeItemRow, String> priceCol = new TableColumn<>("Prix unitaire");
        priceCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(ProductLabels.price(d.getValue().product.unitPrice())));
        TableColumn<ExchangeItemRow, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(ProductLabels.price(d.getValue().total())));
        TableColumn<ExchangeItemRow, Void> removeCol = new TableColumn<>();
        removeCol.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
            private final Button remove = new Button("×");
            {
                remove.getStyleClass().add("button-ghost");
                remove.setOnAction(e -> {
                    exchangeItems.getItems().remove(getTableRow().getItem());
                    updateExchangeSummary();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : remove);
            }
        });
        exchangeItems.getColumns().addAll(nameCol, qtyCol2, priceCol, totalCol, removeCol);
        exchangeItems.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        exchangeItems.setPlaceholder(new EmptyState("Aucun article de remplacement ajouté"));
        exchangeItems.setPrefHeight(200);

        exchangeSummary.getStyleClass().add("page-sub");
        exchangeSummary.setWrapText(true);
        updateExchangeSummary();

        exchangePayMethod.getItems().addAll("CASH", "CARD", "MOBILE_MONEY");
        exchangePayMethod.getSelectionModel().select("CASH");
        Label payHint = new Label("Méthode pour la différence (si due)");
        payHint.getStyleClass().add("form-label");

        Button confirm = new Button("Confirmer l'échange");
        confirm.getStyleClass().add("button-primary");
        confirm.setOnAction(e -> confirmExchange());

        HBox confirmBar = new HBox(10, payHint, exchangePayMethod, confirm);

        VBox box = new VBox(10, title, hint, pickBar, exchangeItems, exchangeSummary, confirmBar);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));
        return box;
    }

    private void searchSales() {
        String q = search.getText() == null ? "" : search.getText().trim();
        loading.setLoading(true);
        FxAsync.run(() -> pos.searchRefundable(q, 50), list -> {
            loading.setLoading(false);
            error.hide();
            salesPager.setItems(list);
            detail.setText(list.size() + (q.isEmpty() ? " vente(s) récente(s)" : " résultat(s)"));
        }, this::fail);
    }

    private void loadReturnable(JsonNode saleNode) {
        selectedReturnable = null;
        lines.getItems().clear();
        selectAll.setSelected(false);
        if (saleNode == null || !saleNode.hasNonNull("id")) {
            return;
        }
        long id = saleNode.get("id").asLong();
        loading.setLoading(true);
        FxAsync.run(() -> pos.returnableSale(id), node -> {
            loading.setLoading(false);
            selectedReturnable = node;
            detail.setText("Vente " + text(node, "saleNumber")
                    + " — remboursable " + money(node, "amountRefundable")
                    + " (déjà remboursé " + money(node, "amountAlreadyRefunded") + ")");
            List<LineRow> rows = new ArrayList<>();
            if (node.hasNonNull("lines") && node.get("lines").isArray()) {
                for (JsonNode lineNode : node.get("lines")) {
                    LineRow row = new LineRow(lineNode);
                    row.selected.addListener((o, was, is) -> updateExchangeSummary());
                    rows.add(row);
                }
            }
            lines.getItems().setAll(rows);
            updateExchangeSummary();
        }, this::fail);
    }

    /** Lignes cochées dans le tableau de retour, au format attendu par l'API (retour classique ou échange). */
    private List<Map<String, Object>> buildSelectedReturnLines() {
        List<Map<String, Object>> selectedLines = new ArrayList<>();
        for (LineRow row : lines.getItems()) {
            if (!row.selected.get()) {
                continue;
            }
            BigDecimal qty;
            try {
                qty = new BigDecimal(row.quantityText.get().trim().replace(',', '.'));
            } catch (Exception e) {
                error.show("Quantité invalide pour « " + row.label + " ».");
                return null;
            }
            if (qty.compareTo(BigDecimal.ZERO) <= 0 || qty.compareTo(row.quantityReturnable) > 0) {
                error.show("Quantité invalide pour « " + row.label + " » (max " + plain(row.quantityReturnable) + ").");
                return null;
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("saleLineId", row.saleLineId);
            line.put("quantity", qty);
            line.put("restock", row.restock.get());
            selectedLines.add(line);
        }
        return selectedLines;
    }

    private void createAndValidate() {
        if (selectedReturnable == null || !selectedReturnable.hasNonNull("id")) {
            error.show("Sélectionnez une vente remboursable.");
            return;
        }
        long saleId = selectedReturnable.get("id").asLong();

        List<Map<String, Object>> selectedLines = buildSelectedReturnLines();
        if (selectedLines == null) {
            return;
        }
        if (selectedLines.isEmpty()) {
            error.show("Sélectionnez au moins une ligne à rembourser.");
            return;
        }

        String motif = reason.getText();
        String method = payMethod.getValue() == null ? "CASH" : payMethod.getValue();
        loading.setLoading(true);
        FxAsync.run(() -> pos.createReturn(saleId, motif, selectedLines), created -> {
            loading.setLoading(false);
            long returnId = created.get("id").asLong();
            BigDecimal total = created.hasNonNull("totalAmount")
                    ? new BigDecimal(created.get("totalAmount").asText())
                    : BigDecimal.ZERO;
            validateReturn(returnId, method, total, null, null, null, null);
        }, this::fail);
    }

    private void validateReturn(long returnId, String method, BigDecimal total,
                                 String managerEmail, String managerPassword,
                                 String managerBadgeCode, String managerPin) {
        loading.setLoading(true);
        FxAsync.run(() -> pos.validateReturn(returnId, method, total, managerEmail, managerPassword,
                managerBadgeCode, managerPin), validated -> {
            loading.setLoading(false);
            error.hide();
            javafx.scene.control.Alert done = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Retour " + text(validated, "refundNumber") + " validé — "
                            + money(validated, "totalAmount"));
            done.setHeaderText("Retour");
            done.showAndWait();
            selectedReturnable = null;
            lines.getItems().clear();
            selectAll.setSelected(false);
            searchSales();
        }, t -> {
            loading.setLoading(false);
            if (t instanceof ApiException api && !api.isUnauthorized()
                    && ApiException.userMessage(api).contains("Validation manager obligatoire")) {
                promptManagerApproval(returnId, method, total);
            } else {
                fail(t);
            }
        });
    }

    private void promptManagerApproval(long returnId, String method, BigDecimal total) {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Validation manager requise");
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        Label info = new Label("Ce remboursement (" + ProductLabels.price(total)
                + ") dépasse le seuil autorisé — validation d'un manager obligatoire.");
        info.setWrapText(true);
        info.getStyleClass().add("page-sub");
        TextField managerEmail = new TextField();
        managerEmail.setPromptText("Email manager");
        javafx.scene.control.PasswordField managerPassword = new javafx.scene.control.PasswordField();
        managerPassword.setPromptText("Mot de passe manager");
        TextField managerBadge = new TextField();
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

        Button confirm = new Button("Valider le retour");
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
            validateReturn(returnId, method, total,
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

    private void searchExchangeProducts() {
        String q = exchangeSearch.getText() == null ? "" : exchangeSearch.getText().trim();
        if (q.isBlank()) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.search(q, 15), results -> {
            loading.setLoading(false);
            exchangeResults.getItems().setAll(results);
            if (!results.isEmpty()) {
                exchangeResults.getSelectionModel().selectFirst();
            }
        }, this::fail);
    }

    private void addExchangeItem() {
        PosProduct product = exchangeResults.getValue();
        if (product == null) {
            error.show("Recherchez et sélectionnez un article de remplacement.");
            return;
        }
        BigDecimal qty;
        try {
            qty = new BigDecimal(exchangeQty.getText().trim().replace(',', '.'));
        } catch (Exception e) {
            error.show("Quantité invalide.");
            return;
        }
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            error.show("Quantité invalide.");
            return;
        }
        error.hide();
        exchangeItems.getItems().add(new ExchangeItemRow(product, qty));
        updateExchangeSummary();
    }

    private void updateExchangeSummary() {
        BigDecimal returnTotal = BigDecimal.ZERO;
        for (LineRow row : lines.getItems()) {
            if (row.selected.get()) {
                returnTotal = returnTotal.add(row.maxRefundAmount);
            }
        }
        BigDecimal newTotal = exchangeItems.getItems().stream()
                .map(ExchangeItemRow::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = newTotal.subtract(returnTotal);
        String netLabel = net.compareTo(BigDecimal.ZERO) > 0
                ? ProductLabels.price(net) + " à payer par le client"
                : net.compareTo(BigDecimal.ZERO) < 0
                        ? ProductLabels.price(net.abs()) + " à rembourser au client"
                        : "aucune différence (échange pur)";
        exchangeSummary.setText("Reprise : " + ProductLabels.price(returnTotal)
                + " · Nouveaux articles : " + ProductLabels.price(newTotal) + " · " + netLabel);
    }

    private void confirmExchange() {
        if (selectedReturnable == null || !selectedReturnable.hasNonNull("id")) {
            error.show("Sélectionnez une vente remboursable.");
            return;
        }
        List<Map<String, Object>> returnLines = buildSelectedReturnLines();
        if (returnLines == null) {
            return;
        }
        if (returnLines.isEmpty()) {
            error.show("Cochez au moins une ligne à reprendre pour l'échange.");
            return;
        }
        if (exchangeItems.getItems().isEmpty()) {
            error.show("Ajoutez au moins un article de remplacement.");
            return;
        }
        long saleId = selectedReturnable.get("id").asLong();
        List<Map<String, Object>> newLines = new ArrayList<>();
        for (ExchangeItemRow row : exchangeItems.getItems()) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("productId", row.product.id());
            line.put("quantityInput", row.qty);
            newLines.add(line);
        }
        String motif = reason.getText();
        String method = exchangePayMethod.getValue() == null ? "CASH" : exchangePayMethod.getValue();
        submitExchange(saleId, returnLines, newLines, motif, method, null, null, null, null);
    }

    private void submitExchange(long saleId, List<Map<String, Object>> returnLines, List<Map<String, Object>> newLines,
                                String motif, String method, String managerEmail, String managerPassword,
                                String managerBadgeCode, String managerPin) {
        loading.setLoading(true);
        FxAsync.run(() -> pos.exchange(saleId, returnLines, newLines, motif, method, managerEmail, managerPassword,
                managerBadgeCode, managerPin), validated -> {
            loading.setLoading(false);
            error.hide();
            javafx.scene.control.Alert done = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Échange " + text(validated, "exchangeNumber") + " confirmé — reprise "
                            + money(validated, "returnTotal") + ", nouveaux articles " + money(validated, "newItemsTotal"));
            done.setHeaderText("Échange");
            done.showAndWait();
            selectedReturnable = null;
            lines.getItems().clear();
            selectAll.setSelected(false);
            exchangeItems.getItems().clear();
            updateExchangeSummary();
            searchSales();
        }, t -> {
            loading.setLoading(false);
            if (t instanceof ApiException api && !api.isUnauthorized()
                    && ApiException.userMessage(api).contains("Validation manager obligatoire")) {
                promptExchangeManagerApproval(saleId, returnLines, newLines, motif, method);
            } else {
                fail(t);
            }
        });
    }

    private void promptExchangeManagerApproval(long saleId, List<Map<String, Object>> returnLines,
                                                List<Map<String, Object>> newLines, String motif, String method) {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Validation manager requise");
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        Label info = new Label("La reprise de cet échange dépasse le seuil autorisé — validation d'un manager obligatoire.");
        info.setWrapText(true);
        info.getStyleClass().add("page-sub");
        TextField managerEmail = new TextField();
        managerEmail.setPromptText("Email manager");
        javafx.scene.control.PasswordField managerPassword = new javafx.scene.control.PasswordField();
        managerPassword.setPromptText("Mot de passe manager");
        TextField managerBadge = new TextField();
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

        Button confirm = new Button("Valider l'échange");
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
            submitExchange(saleId, returnLines, newLines, motif, method,
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

    private void fail(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api && !api.isUnauthorized()) {
            error.show(ApiException.userMessage(api));
        } else if (!(t instanceof ApiException a && a.isUnauthorized())) {
            error.show("Une erreur est survenue.");
        }
    }

    private static String text(JsonNode n, String f) {
        return n != null && n.hasNonNull(f) ? n.get(f).asText("") : "—";
    }

    private static String money(JsonNode n, String f) {
        if (n == null || !n.hasNonNull(f)) {
            return ProductLabels.price(BigDecimal.ZERO);
        }
        try {
            return ProductLabels.price(new BigDecimal(n.get(f).asText()));
        } catch (Exception e) {
            return n.get(f).asText();
        }
    }

    private static String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal decimal(JsonNode n, String field) {
        if (n == null || !n.hasNonNull(field)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(n.get(field).asText());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static TableColumn<JsonNode, String> col(String title, java.util.function.Function<JsonNode, String> fn) {
        TableColumn<JsonNode, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return c;
    }

    /** Une ligne de vente remboursable, avec l'état d'édition choisi par le caissier. */
    private static final class LineRow {
        final long saleLineId;
        final String label;
        final BigDecimal quantitySold;
        final BigDecimal quantityAlreadyReturned;
        final BigDecimal quantityReturnable;
        final BigDecimal maxRefundAmount;
        final BooleanProperty selected = new SimpleBooleanProperty(false);
        final StringProperty quantityText = new SimpleStringProperty("");
        final BooleanProperty restock = new SimpleBooleanProperty(true);

        LineRow(JsonNode n) {
            this.saleLineId = n.path("saleLineId").asLong();
            String product = n.hasNonNull("productNom") ? n.get("productNom").asText() : "";
            String variant = n.hasNonNull("variantNameSnapshot") ? n.get("variantNameSnapshot").asText("") : "";
            String packaging = n.hasNonNull("packagingNameSnapshot") ? n.get("packagingNameSnapshot").asText("") : "";
            StringBuilder sb = new StringBuilder(product.isBlank() ? "—" : product);
            if (!variant.isBlank()) {
                sb.append(" · ").append(variant);
            }
            if (!packaging.isBlank()) {
                sb.append(" · ").append(packaging);
            }
            this.label = sb.toString();
            this.quantitySold = decimal(n, "quantitySold");
            this.quantityAlreadyReturned = decimal(n, "quantityAlreadyReturned");
            this.quantityReturnable = decimal(n, "quantityReturnable");
            this.maxRefundAmount = decimal(n, "maxRefundAmount");

            selected.addListener((o, was, is) -> {
                if (is && !canReturn()) {
                    selected.set(false);
                    return;
                }
                if (is && (quantityText.get() == null || quantityText.get().isBlank())) {
                    quantityText.set(plain(quantityReturnable));
                }
            });
        }

        boolean canReturn() {
            return quantityReturnable != null && quantityReturnable.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    /** Article de remplacement ajouté à un échange. */
    private static final class ExchangeItemRow {
        final PosProduct product;
        final BigDecimal qty;

        ExchangeItemRow(PosProduct product, BigDecimal qty) {
            this.product = product;
            this.qty = qty;
        }

        BigDecimal total() {
            BigDecimal price = product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
            return price.multiply(qty);
        }
    }
}
