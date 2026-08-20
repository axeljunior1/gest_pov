package com.gestpov.desktop.ui.units;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.Unit;
import com.gestpov.desktop.model.UnitConversion;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.UnitClient;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Unités + conversions globales (API sans PUT rename — édition = resaisie nom/symbole avant recreate locale).
 * MVP : sélection pour préremplir le formulaire (create), conversions list/create/delete, convert preview.
 */
public final class UnitsView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final UnitClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<Unit> table = new TableView<>();
    private final ListPager<Unit> pager = new ListPager<>(table);
    private final TableView<UnitConversion> conversionsTable = new TableView<>();
    private final TextField nom = new TextField();
    private final TextField symbole = new TextField();
    private final TextField search = new TextField();
    private final Label countLabel = new Label();
    private final ComboBox<Unit> fromUnit = new ComboBox<>();
    private final ComboBox<Unit> toUnit = new ComboBox<>();
    private final TextField factor = new TextField();
    private final TextField convertQty = new TextField();
    private final Label convertResult = new Label();
    private List<Unit> all = List.of();
    private Long editingId;

    public UnitsView(SessionContext session) {
        this.session = session;
        this.client = new UnitClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Unités");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Unités de base et conversions globales (kg↔g, L↔mL…)");
        sub.getStyleClass().add("page-sub");
        countLabel.getStyleClass().add("page-sub");

        nom.setPromptText("Nom *");
        symbole.setPromptText("Symbole *");
        Button create = new Button("Créer");
        create.getStyleClass().add("button-primary");
        create.setOnAction(e -> create());
        Button cancelEdit = new Button("Annuler");
        cancelEdit.getStyleClass().add("button-ghost");
        cancelEdit.setOnAction(e -> resetForm());
        HBox form = new HBox(8, nom, symbole, create, cancelEdit);
        form.setAlignment(Pos.CENTER_LEFT);
        boolean canCreate = session.hasPermission("products.create");
        form.setVisible(canCreate);
        form.setManaged(canCreate);
        HBox.setHgrow(nom, Priority.ALWAYS);

        search.setPromptText("Filtrer par nom ou symbole…");
        search.textProperty().addListener((o, a, b) -> applyFilter());
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());
        HBox searchBar = new HBox(8, search, refresh);
        HBox.setHgrow(search, Priority.ALWAYS);

        VBox card = new VBox(10, form, searchBar);
        card.getStyleClass().add("card");

        table.setPlaceholder(new EmptyState("Aucune unité"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(col("Nom", Unit::nom));
        table.getColumns().add(col("Symbole", Unit::symbole));
        TableColumn<Unit, Void> actions = new TableColumn<>();
        actions.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Unit unit = getTableRow().getItem();
                HBox box = new HBox(6);
                if (session.hasPermission("products.create")) {
                    Button edit = new Button("Éditer");
                    edit.getStyleClass().add("button-ghost");
                    edit.setOnAction(e -> beginEdit(unit));
                    box.getChildren().add(edit);
                }
                if (session.hasPermission("products.delete")) {
                    Button del = new Button("Suppr.");
                    del.getStyleClass().add("button-danger");
                    del.setOnAction(e -> delete(unit));
                    box.getChildren().add(del);
                }
                setGraphic(box.getChildren().isEmpty() ? null : box);
            }
        });
        table.getColumns().add(actions);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                beginEdit(b);
            }
        });

        VBox conversionsCard = buildConversionsCard();

        VBox page = new VBox(16, title, sub, countLabel, error, card, table, conversionsCard, pager.bar());
        VBox.setVgrow(table, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private VBox buildConversionsCard() {
        Label h = new Label("Conversions globales");
        h.getStyleClass().add("settings-group-title");
        fromUnit.setPromptText("De");
        toUnit.setPromptText("Vers");
        factor.setPromptText("Facteur *");
        Button add = new Button("Ajouter conversion");
        add.getStyleClass().add("button-secondary");
        add.setOnAction(e -> createConversion());
        boolean canCreate = session.hasPermission("products.create");
        HBox convForm = new HBox(8, fromUnit, toUnit, factor, add);
        convForm.setAlignment(Pos.CENTER_LEFT);
        convForm.setVisible(canCreate);
        convForm.setManaged(canCreate);

        convertQty.setPromptText("Quantité");
        Button convertBtn = new Button("Convertir");
        convertBtn.getStyleClass().add("button-ghost");
        convertBtn.setOnAction(e -> previewConvert());
        convertResult.getStyleClass().add("page-sub");
        HBox preview = new HBox(8, convertQty, convertBtn, convertResult);
        preview.setAlignment(Pos.CENTER_LEFT);

        conversionsTable.setItems(FXCollections.observableArrayList());
        conversionsTable.setPlaceholder(new EmptyState("Aucune conversion"));
        conversionsTable.setPrefHeight(160);
        conversionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        conversionsTable.getColumns().add(colConv("De", c -> c.fromUnitSymbole()));
        conversionsTable.getColumns().add(colConv("Vers", c -> c.toUnitSymbole()));
        conversionsTable.getColumns().add(colConv("Facteur", c ->
                c.factor() == null ? "" : c.factor().toPlainString()));
        if (session.hasPermission("products.delete")) {
            TableColumn<UnitConversion, Void> actions = new TableColumn<>();
            actions.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        return;
                    }
                    UnitConversion conv = getTableRow().getItem();
                    Button del = new Button("Suppr.");
                    del.getStyleClass().add("button-danger");
                    del.setOnAction(e -> deleteConversion(conv));
                    setGraphic(del);
                }
            });
            conversionsTable.getColumns().add(actions);
        }

        Label note = new Label("Pas d'API rename unité : « Éditer » préremplit pour créer une nouvelle unité.");
        note.getStyleClass().add("page-sub");
        VBox card = new VBox(10, h, note, convForm, preview, conversionsTable);
        card.getStyleClass().add("card");
        return card;
    }

    private void beginEdit(Unit unit) {
        editingId = unit.id();
        nom.setText(unit.nom());
        symbole.setText(unit.symbole());
    }

    private void resetForm() {
        editingId = null;
        nom.clear();
        symbole.clear();
    }

    private void create() {
        if (blank(nom) || blank(symbole)) {
            error.show("Le nom et le symbole sont obligatoires.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.create(nom.getText().trim(), symbole.getText().trim()), created -> {
            resetForm();
            reload();
        }, this::fail);
    }

    private void delete(Unit unit) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer", "Supprimer cette unité ?")) {
            return;
        }
        loading.setLoading(true);
        FxAsync.runVoid(() -> client.delete(unit.id()), this::reload, this::fail);
    }

    private void createConversion() {
        Unit from = fromUnit.getValue();
        Unit to = toUnit.getValue();
        if (from == null || to == null || from.id() == null || to.id() == null) {
            error.show("Sélectionnez les deux unités.");
            return;
        }
        BigDecimal f;
        try {
            f = new BigDecimal(factor.getText().trim());
        } catch (Exception e) {
            error.show("Facteur invalide.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.createConversion(from.id(), to.id(), f), created -> {
            factor.clear();
            reload();
        }, this::fail);
    }

    private void deleteConversion(UnitConversion conv) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer", "Supprimer cette conversion ?")) {
            return;
        }
        loading.setLoading(true);
        FxAsync.runVoid(() -> client.deleteConversion(conv.id()), this::reload, this::fail);
    }

    private void previewConvert() {
        Unit from = fromUnit.getValue();
        Unit to = toUnit.getValue();
        if (from == null || to == null) {
            convertResult.setText("Sélectionnez De / Vers");
            return;
        }
        BigDecimal qty;
        try {
            qty = new BigDecimal(convertQty.getText().trim());
        } catch (Exception e) {
            convertResult.setText("Quantité invalide");
            return;
        }
        FxAsync.run(() -> client.convert(from.id(), to.id(), qty), result -> {
            convertResult.setText(result == null ? "—" : "= " + result.toPlainString() + " " + to.symbole());
        }, this::fail);
    }

    @Override
    public void reload() {
        loading.setLoading(true);
        FxAsync.run(() -> {
            List<Unit> units = client.findAll();
            List<UnitConversion> conversions = client.listConversions();
            return new Payload(units, conversions);
        }, payload -> {
            loading.setLoading(false);
            all = payload.units();
            applyFilter();
            fromUnit.getItems().setAll(all);
            toUnit.getItems().setAll(all);
            conversionsTable.getItems().setAll(payload.conversions());
        }, this::fail);
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<Unit> filtered = q.isEmpty() ? all : all.stream().filter(u ->
                (u.nom() != null && u.nom().toLowerCase(Locale.ROOT).contains(q))
                        || (u.symbole() != null && u.symbole().toLowerCase(Locale.ROOT).contains(q))
        ).collect(Collectors.toList());
        pager.setItems(filtered);
        countLabel.setText(filtered.size() + " unité(s)");
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

    private static TableColumn<Unit, String> col(String title, java.util.function.Function<Unit, String> fn) {
        TableColumn<Unit, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static TableColumn<UnitConversion, String> colConv(String title,
                                                               java.util.function.Function<UnitConversion, String> fn) {
        TableColumn<UnitConversion, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private record Payload(List<Unit> units, List<UnitConversion> conversions) {
    }
}
