package com.gestpov.desktop.ui.stock;

import com.gestpov.desktop.ui.Reloadable;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.StockEntryDoc;
import com.gestpov.desktop.model.StockExitDoc;
import com.gestpov.desktop.model.StockLocation;
import com.gestpov.desktop.model.Warehouse;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.ProductClient;
import com.gestpov.desktop.net.StockClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bons d'entrée (lecture) et bons de sortie (lecture + création : casse, perte, avarie,
 * retour fournisseur, usage interne...). Les sorties liées aux ventes POS sont automatiques.
 */
public final class StockEntriesExitsView extends StackPane implements Reloadable {

    private static final List<String> EXIT_REASONS =
            List.of("INTERNAL_USE", "DAMAGED", "LOST", "DONATION", "RETURN_SUPPLIER", "OTHER");

    private final SessionContext session;
    private final StockClient client;
    private final ProductClient products;
    private final boolean canReadEntries;
    private final boolean canReadExits;
    private final boolean canCreateExits;
    private final boolean canValidateExits;
    private final boolean canCancelExits;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();

    private final ToggleButton tabEntries = new ToggleButton("Entrées");
    private final ToggleButton tabExits = new ToggleButton("Sorties");
    private final TableView<StockEntryDoc> entriesTable = new TableView<>();
    private final TableView<StockExitDoc> exitsTable = new TableView<>();
    private final VBox entriesPane = new VBox(8);
    private final VBox exitsPane = new VBox(8);
    private final Button createExitBtn = new Button("Créer une sortie…");
    private final Button validateExitBtn = new Button("Valider");
    private final Button cancelExitBtn = new Button("Annuler");

    public StockEntriesExitsView(SessionContext session) {
        this.session = session;
        this.client = new StockClient(session.api());
        this.products = new ProductClient(session.api());
        this.canReadEntries = session.hasPermission("stock_entry.read");
        this.canReadExits = session.hasPermission("stock_exit.read");
        this.canCreateExits = session.hasPermission("stock_exit.create");
        this.canValidateExits = session.hasPermission("stock_exit.validate");
        this.canCancelExits = session.hasPermission("stock_exit.cancel");
        getChildren().addAll(build(), loading);
        showSelectedTab();
        reload();
    }

    private VBox build() {
        Label title = new Label("Entrées / Sorties");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Bons d'entrée (lecture) et bons de sortie (casse, perte, avarie, retour fournisseur…)");
        sub.getStyleClass().add("page-sub");

        ToggleGroup tabs = new ToggleGroup();
        styleTab(tabEntries, tabs, canReadEntries);
        styleTab(tabExits, tabs, canReadExits && !canReadEntries);
        tabEntries.setDisable(!canReadEntries);
        tabEntries.setVisible(canReadEntries);
        tabEntries.setManaged(canReadEntries);
        tabExits.setDisable(!canReadExits);
        tabExits.setVisible(canReadExits);
        tabExits.setManaged(canReadExits);
        HBox tabBar = new HBox(8, tabEntries, tabExits);
        tabBar.setAlignment(Pos.CENTER_LEFT);

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        entriesTable.setPlaceholder(new EmptyState("Aucune entrée"));
        entriesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        entriesTable.getColumns().addAll(
                colE("N°", e -> nz(e.entryNumber())),
                colE("Date", e -> nz(e.entryDate())),
                colE("Fournisseur", e -> nz(e.supplierNom())),
                colE("Entrepôt", e -> nz(e.warehouseCode())),
                colE("Statut", e -> nz(e.status())),
                colE("Réf.", e -> nz(e.referenceDocument()))
        );
        VBox.setVgrow(entriesTable, Priority.ALWAYS);
        entriesPane.getChildren().setAll(entriesTable);

        exitsTable.setPlaceholder(new EmptyState("Aucune sortie"));
        exitsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        exitsTable.getColumns().addAll(
                colX("N°", x -> nz(x.exitNumber())),
                colX("Date", x -> nz(x.exitDate())),
                colX("Motif", x -> reasonLabel(x.reason())),
                colX("Entrepôt", x -> nz(x.warehouseCode())),
                colX("Statut", x -> nz(x.status()))
        );
        exitsTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateExitActions(b));
        VBox.setVgrow(exitsTable, Priority.ALWAYS);

        createExitBtn.getStyleClass().addAll("button-primary");
        createExitBtn.setOnAction(e -> openCreateExitDialog());
        createExitBtn.setVisible(canCreateExits);
        createExitBtn.setManaged(canCreateExits);
        validateExitBtn.getStyleClass().add("button-secondary");
        validateExitBtn.setOnAction(e -> validateSelectedExit());
        cancelExitBtn.getStyleClass().add("button-danger");
        cancelExitBtn.setOnAction(e -> cancelSelectedExit());
        HBox exitActions = new HBox(8, createExitBtn, validateExitBtn, cancelExitBtn);
        exitActions.setAlignment(Pos.CENTER_LEFT);
        updateExitActions(null);

        exitsPane.getChildren().setAll(exitActions, exitsTable);
        VBox.setVgrow(exitsTable, Priority.ALWAYS);

        StackPane body = new StackPane(entriesPane, exitsPane);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox page = new VBox(12, title, sub, error, new HBox(12, tabBar, refresh), body);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void updateExitActions(StockExitDoc selected) {
        boolean draft = selected != null && "DRAFT".equals(selected.status());
        boolean validated = selected != null && "VALIDATED".equals(selected.status());
        validateExitBtn.setVisible(canValidateExits);
        validateExitBtn.setManaged(canValidateExits);
        validateExitBtn.setDisable(!draft);
        cancelExitBtn.setVisible(canCancelExits);
        cancelExitBtn.setManaged(canCancelExits);
        cancelExitBtn.setDisable(!(draft || validated));
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
            reload();
        });
    }

    private void showSelectedTab() {
        boolean entries = tabEntries.isSelected() || (!canReadExits && canReadEntries);
        entriesPane.setVisible(entries);
        entriesPane.setManaged(entries);
        exitsPane.setVisible(!entries);
        exitsPane.setManaged(!entries);
    }

    @Override
    public void reload() {
        error.hide();
        if (tabEntries.isSelected() && canReadEntries) {
            loading.setLoading(true);
            FxAsync.run(client::listEntries, list -> {
                loading.setLoading(false);
                entriesTable.getItems().setAll(list);
            }, this::fail);
        } else if (canReadExits) {
            loading.setLoading(true);
            FxAsync.run(client::listExits, list -> {
                loading.setLoading(false);
                exitsTable.getItems().setAll(list);
                updateExitActions(null);
            }, this::fail);
        }
    }

    private void validateSelectedExit() {
        StockExitDoc selected = exitsTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.id() == null) {
            error.show("Sélectionnez une sortie brouillon.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.validateExit(selected.id()), node -> {
            loading.setLoading(false);
            error.hide();
            reload();
        }, this::fail);
    }

    private void cancelSelectedExit() {
        StockExitDoc selected = exitsTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.id() == null) {
            error.show("Sélectionnez une sortie.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.cancelExit(selected.id()), node -> {
            loading.setLoading(false);
            error.hide();
            reload();
        }, this::fail);
    }

    private void openCreateExitDialog() {
        loading.setLoading(true);
        FxAsync.run(() -> new Refs(client.listWarehouses(), products.search(new com.gestpov.desktop.model.ProductQuery())),
                refs -> {
                    loading.setLoading(false);
                    showCreateExitDialog(refs);
                }, this::fail);
    }

    private void showCreateExitDialog(Refs refs) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Créer une sortie de stock");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        ComboBox<String> reasonCombo = new ComboBox<>();
        reasonCombo.getItems().setAll(EXIT_REASONS);
        reasonCombo.setConverter(reasonConverter());
        reasonCombo.getSelectionModel().selectFirst();
        reasonCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Warehouse> whCombo = new ComboBox<>();
        whCombo.getItems().setAll(refs.warehouses());
        whCombo.setMaxWidth(Double.MAX_VALUE);
        ComboBox<StockLocation> locCombo = new ComboBox<>();
        locCombo.setMaxWidth(Double.MAX_VALUE);
        whCombo.valueProperty().addListener((o, a, b) -> {
            locCombo.getItems().clear();
            if (b != null && b.id() != null) {
                FxAsync.run(() -> client.listLocations(b.id()), locs -> {
                    locCombo.getItems().setAll(locs);
                    if (!locs.isEmpty()) {
                        locCombo.getSelectionModel().selectFirst();
                    }
                }, this::fail);
            }
        });
        if (!refs.warehouses().isEmpty()) {
            whCombo.getSelectionModel().selectFirst();
        }

        TextField notesField = new TextField();
        notesField.setPromptText("Notes (optionnel)");

        VBox linesBox = new VBox(8);
        record LineRow(ComboBox<ProductOption> product, TextField qty) {
        }
        List<LineRow> rows = new java.util.ArrayList<>();
        Runnable addRow = () -> {
            ComboBox<ProductOption> productCombo = new ComboBox<>();
            productCombo.getItems().setAll(refs.products());
            productCombo.setMaxWidth(260);
            TextField qtyField = new TextField("1");
            qtyField.setPrefWidth(80);
            Button removeBtn = new Button("×");
            removeBtn.getStyleClass().add("button-ghost");
            HBox row = new HBox(8, productCombo, qtyField, removeBtn);
            row.setAlignment(Pos.CENTER_LEFT);
            LineRow lineRow = new LineRow(productCombo, qtyField);
            removeBtn.setOnAction(ev -> {
                rows.remove(lineRow);
                linesBox.getChildren().remove(row);
            });
            rows.add(lineRow);
            linesBox.getChildren().add(row);
        };
        Button addLineBtn = new Button("+ Ajouter une ligne");
        addLineBtn.getStyleClass().addAll("button-secondary", "pos-action-sm");
        addLineBtn.setOnAction(e -> addRow.run());
        addRow.run();

        Button confirm = new Button("Créer (brouillon)");
        confirm.getStyleClass().add("button-primary");
        confirm.setOnAction(e -> {
            Warehouse wh = whCombo.getValue();
            StockLocation loc = locCombo.getValue();
            if (wh == null || wh.id() == null || loc == null || loc.id() == null) {
                error.show("Choisissez un entrepôt et un emplacement.");
                return;
            }
            List<Map<String, Object>> lines = new java.util.ArrayList<>();
            for (LineRow r : rows) {
                ProductOption p = r.product().getValue();
                if (p == null) {
                    continue;
                }
                BigDecimal qty;
                try {
                    qty = new BigDecimal(r.qty().getText().trim().replace(',', '.'));
                } catch (Exception ex) {
                    error.show("Quantité invalide pour « " + p.nom() + " ».");
                    return;
                }
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    error.show("Quantité invalide pour « " + p.nom() + " ».");
                    return;
                }
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("productId", p.id());
                line.put("quantityInput", qty);
                lines.add(line);
            }
            if (lines.isEmpty()) {
                error.show("Ajoutez au moins une ligne de produit.");
                return;
            }
            String reason = reasonCombo.getValue() == null ? "OTHER" : reasonCombo.getValue();
            String notes = notesField.getText();
            loading.setLoading(true);
            FxAsync.run(() -> client.createExit(wh.id(), loc.id(), reason, notes, lines), created -> {
                loading.setLoading(false);
                error.hide();
                dialog.close();
                Alert done = new Alert(Alert.AlertType.INFORMATION,
                        "Sortie créée en brouillon — sélectionnez-la puis cliquez « Valider » pour décrémenter le stock.");
                done.setHeaderText("Bon de sortie créé");
                done.showAndWait();
                tabExits.setSelected(true);
                showSelectedTab();
                reload();
            }, t -> {
                loading.setLoading(false);
                fail(t);
            });
        });

        VBox content = new VBox(10,
                labeled("Motif", reasonCombo),
                labeled("Entrepôt", whCombo),
                labeled("Emplacement", locCombo),
                new Label("Lignes"),
                linesBox,
                addLineBtn,
                labeled("Notes", notesField),
                confirm
        );
        content.setPadding(new Insets(10));
        content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
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

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String reasonLabel(String reason) {
        if (reason == null) {
            return "—";
        }
        return switch (reason) {
            case "SALE" -> "Vente";
            case "INTERNAL_USE" -> "Usage interne";
            case "DAMAGED" -> "Casse / avarie";
            case "LOST" -> "Perte";
            case "DONATION" -> "Don";
            case "RETURN_SUPPLIER" -> "Retour fournisseur";
            case "OTHER" -> "Autre";
            default -> reason;
        };
    }

    private static javafx.util.StringConverter<String> reasonConverter() {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : reasonLabel(value);
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        };
    }

    private static TableColumn<StockEntryDoc, String> colE(String title,
                                                          java.util.function.Function<StockEntryDoc, String> fn) {
        TableColumn<StockEntryDoc, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static TableColumn<StockExitDoc, String> colX(String title,
                                                         java.util.function.Function<StockExitDoc, String> fn) {
        TableColumn<StockExitDoc, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private record Refs(List<Warehouse> warehouses, List<Product> productsRaw) {
        List<ProductOption> products() {
            return productsRaw.stream().map(p -> new ProductOption(p.id(), p.nom(), p.sku())).toList();
        }
    }

    private record ProductOption(Long id, String nom, String sku) {
        @Override
        public String toString() {
            return sku == null || sku.isBlank() ? nom : nom + " (" + sku + ")";
        }
    }
}
