package com.gestpov.desktop.ui.stock;

import com.gestpov.desktop.ui.Reloadable;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.StockEntryDoc;
import com.gestpov.desktop.model.StockExitDoc;
import com.gestpov.desktop.model.StockLocation;
import com.gestpov.desktop.model.Supplier;
import com.gestpov.desktop.model.Warehouse;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.ProductClient;
import com.gestpov.desktop.net.StockClient;
import com.gestpov.desktop.net.SupplierClient;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bons d'entrée (réception fournisseur, multi-produits, pièces jointes facture) et bons de
 * sortie (casse, perte, avarie, retour fournisseur, usage interne...). Cycle brouillon →
 * validé → annulé des deux côtés ; les sorties liées aux ventes POS sont automatiques.
 * Double-clic (ou bouton « Détails ») sur une ligne ouvre le détail complet en popup.
 */
public final class StockEntriesExitsView extends StackPane implements Reloadable {

    private static final List<String> EXIT_REASONS =
            List.of("INTERNAL_USE", "DAMAGED", "LOST", "DONATION", "RETURN_SUPPLIER", "OTHER");

    private final SessionContext session;
    private final StockClient client;
    private final ProductClient products;
    private final SupplierClient suppliers;
    private final boolean canReadEntries;
    private final boolean canCreateEntries;
    private final boolean canValidateEntries;
    private final boolean canCancelEntries;
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

    private final Button createEntryBtn = new Button("Créer une entrée…");
    private final Button entryDetailsBtn = new Button("Détails…");
    private final Button validateEntryBtn = new Button("Valider");
    private final Button cancelEntryBtn = new Button("Annuler");

    private final Button createExitBtn = new Button("Créer une sortie…");
    private final Button exitDetailsBtn = new Button("Détails…");
    private final Button validateExitBtn = new Button("Valider");
    private final Button cancelExitBtn = new Button("Annuler");

    public StockEntriesExitsView(SessionContext session) {
        this.session = session;
        this.client = new StockClient(session.api());
        this.products = new ProductClient(session.api());
        this.suppliers = new SupplierClient(session.api());
        this.canReadEntries = session.hasPermission("stock_entry.read");
        this.canCreateEntries = session.hasPermission("stock_entry.create");
        this.canValidateEntries = session.hasPermission("stock_entry.validate");
        this.canCancelEntries = session.hasPermission("stock_entry.cancel");
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
        Label sub = new Label("Réceptions fournisseur (multi-produits, facture jointe) et sorties "
                + "(casse, perte, avarie, retour fournisseur…) — double-clic sur une ligne pour le détail");
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

        buildEntriesPane();
        buildExitsPane();

        StackPane body = new StackPane(entriesPane, exitsPane);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox page = new VBox(12, title, sub, error, new HBox(12, tabBar, refresh), body);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void buildEntriesPane() {
        entriesTable.setPlaceholder(new EmptyState("Aucune entrée — réceptionnez un achat fournisseur"));
        entriesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        entriesTable.getColumns().addAll(
                colE("N°", e -> nz(e.entryNumber())),
                colE("Date", e -> dateTime(e.createdAt(), e.entryDate())),
                colE("Fournisseur", e -> nz(e.supplierNom())),
                colE("Entrepôt", e -> nz(e.warehouseCode())),
                colE("Statut", e -> nz(e.status())),
                colE("Réf.", e -> nz(e.referenceDocument()))
        );
        entriesTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateEntryActions(b));
        entriesTable.setRowFactory(tv -> {
            TableRow<StockEntryDoc> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty() && row.getItem().id() != null) {
                    showEntryDetail(row.getItem().id());
                }
            });
            return row;
        });
        VBox.setVgrow(entriesTable, Priority.ALWAYS);

        createEntryBtn.getStyleClass().add("button-primary");
        createEntryBtn.setOnAction(e -> openCreateEntryDialog());
        createEntryBtn.setVisible(canCreateEntries);
        createEntryBtn.setManaged(canCreateEntries);
        entryDetailsBtn.getStyleClass().add("button-secondary");
        entryDetailsBtn.setOnAction(e -> {
            StockEntryDoc selected = entriesTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.id() != null) {
                showEntryDetail(selected.id());
            }
        });
        validateEntryBtn.getStyleClass().add("button-secondary");
        validateEntryBtn.setOnAction(e -> validateSelectedEntry());
        cancelEntryBtn.getStyleClass().add("button-danger");
        cancelEntryBtn.setOnAction(e -> cancelSelectedEntry());
        HBox entryActions = new HBox(8, createEntryBtn, entryDetailsBtn, validateEntryBtn, cancelEntryBtn);
        entryActions.setAlignment(Pos.CENTER_LEFT);
        updateEntryActions(null);

        entriesPane.getChildren().setAll(entryActions, entriesTable);
        VBox.setVgrow(entriesTable, Priority.ALWAYS);
    }

    private void buildExitsPane() {
        exitsTable.setPlaceholder(new EmptyState("Aucune sortie"));
        exitsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        exitsTable.getColumns().addAll(
                colX("N°", x -> nz(x.exitNumber())),
                colX("Date", x -> dateTime(x.createdAt(), x.exitDate())),
                colX("Motif", x -> reasonLabel(x.reason())),
                colX("Entrepôt", x -> nz(x.warehouseCode())),
                colX("Statut", x -> nz(x.status()))
        );
        exitsTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateExitActions(b));
        exitsTable.setRowFactory(tv -> {
            TableRow<StockExitDoc> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty() && row.getItem().id() != null) {
                    showExitDetail(row.getItem().id());
                }
            });
            return row;
        });
        VBox.setVgrow(exitsTable, Priority.ALWAYS);

        createExitBtn.getStyleClass().addAll("button-primary");
        createExitBtn.setOnAction(e -> openCreateExitDialog());
        createExitBtn.setVisible(canCreateExits);
        createExitBtn.setManaged(canCreateExits);
        exitDetailsBtn.getStyleClass().add("button-secondary");
        exitDetailsBtn.setOnAction(e -> {
            StockExitDoc selected = exitsTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.id() != null) {
                showExitDetail(selected.id());
            }
        });
        validateExitBtn.getStyleClass().add("button-secondary");
        validateExitBtn.setOnAction(e -> validateSelectedExit());
        cancelExitBtn.getStyleClass().add("button-danger");
        cancelExitBtn.setOnAction(e -> cancelSelectedExit());
        HBox exitActions = new HBox(8, createExitBtn, exitDetailsBtn, validateExitBtn, cancelExitBtn);
        exitActions.setAlignment(Pos.CENTER_LEFT);
        updateExitActions(null);

        exitsPane.getChildren().setAll(exitActions, exitsTable);
        VBox.setVgrow(exitsTable, Priority.ALWAYS);
    }

    private void updateEntryActions(StockEntryDoc selected) {
        boolean draft = selected != null && "DRAFT".equals(selected.status());
        boolean validated = selected != null && "VALIDATED".equals(selected.status());
        entryDetailsBtn.setDisable(selected == null);
        validateEntryBtn.setVisible(canValidateEntries);
        validateEntryBtn.setManaged(canValidateEntries);
        validateEntryBtn.setDisable(!draft);
        cancelEntryBtn.setVisible(canCancelEntries);
        cancelEntryBtn.setManaged(canCancelEntries);
        cancelEntryBtn.setDisable(!(draft || validated));
    }

    private void updateExitActions(StockExitDoc selected) {
        boolean draft = selected != null && "DRAFT".equals(selected.status());
        boolean validated = selected != null && "VALIDATED".equals(selected.status());
        exitDetailsBtn.setDisable(selected == null);
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
                updateEntryActions(null);
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

    // ---- Entrées ----

    private void validateSelectedEntry() {
        StockEntryDoc selected = entriesTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.id() == null) {
            error.show("Sélectionnez une entrée brouillon.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.validateEntry(selected.id()), node -> {
            loading.setLoading(false);
            error.hide();
            reload();
        }, this::fail);
    }

    private void cancelSelectedEntry() {
        StockEntryDoc selected = entriesTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.id() == null) {
            error.show("Sélectionnez une entrée.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> client.cancelEntry(selected.id()), node -> {
            loading.setLoading(false);
            error.hide();
            reload();
        }, this::fail);
    }

    private void showEntryDetail(long id) {
        loading.setLoading(true);
        FxAsync.run(() -> client.getEntry(id), node -> {
            loading.setLoading(false);
            openEntryDetailDialog(node);
        }, this::fail);
    }

    private void openEntryDetailDialog(JsonNode entry) {
        long entryId = entry.path("id").asLong();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Entrée " + entry.path("entryNumber").asText("—"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        GridPane header = detailGrid();
        int row = 0;
        row = addDetailRow(header, row, "N°", entry.path("entryNumber").asText("—"));
        row = addDetailRow(header, row, "Date", entry.path("entryDate").asText("—"));
        row = addDetailRow(header, row, "Fournisseur", textOr(entry, "supplierNom", "—"));
        row = addDetailRow(header, row, "Entrepôt", textOr(entry, "warehouseCode", "—")
                + " / " + textOr(entry, "locationCode", "—"));
        row = addDetailRow(header, row, "Référence facture", textOr(entry, "referenceDocument", "—"));
        row = addDetailRow(header, row, "Statut", statusLabel(entry.path("status").asText(null)));
        row = addDetailRow(header, row, "Créé par", textOr(entry, "createdBy", "—"));
        if (entry.hasNonNull("validatedBy")) {
            row = addDetailRow(header, row, "Validé par", entry.path("validatedBy").asText("—"));
        }
        if (entry.hasNonNull("cancelledBy")) {
            row = addDetailRow(header, row, "Annulé par", entry.path("cancelledBy").asText("—"));
        }
        if (entry.hasNonNull("notes") && !entry.path("notes").asText("").isBlank()) {
            addDetailRow(header, row, "Notes", entry.path("notes").asText());
        }

        TableView<JsonNode> linesTable = new TableView<>();
        linesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        linesTable.setPlaceholder(new EmptyState("Aucune ligne"));
        linesTable.getColumns().addAll(
                lineCol("Produit", l -> textOr(l, "productNom", "—")),
                lineCol("Quantité", l -> textOr(l, "quantityInput", "—") + " " + textOr(l, "unitSymbole", "")),
                lineCol("Coût unitaire", l -> textOr(l, "unitCost", "—")),
                lineCol("Lot", l -> textOr(l, "lotNumber", "—")),
                lineCol("Péremption", l -> textOr(l, "expiryDate", "—")),
                lineCol("Notes", l -> textOr(l, "notes", ""))
        );
        JsonNode lignes = entry.get("lignes");
        if (lignes != null && lignes.isArray()) {
            lignes.forEach(linesTable.getItems()::add);
        }
        linesTable.setPrefHeight(Math.min(260, 40 + linesTable.getItems().size() * 28));

        Label attachmentsTitle = new Label("Pièces jointes");
        attachmentsTitle.getStyleClass().add("pos-section-label");
        VBox attachmentsList = new VBox(6);
        Button addAttachmentBtn = new Button("Ajouter une facture…");
        addAttachmentBtn.getStyleClass().addAll("button-secondary", "pos-action-sm");
        addAttachmentBtn.setVisible(canCreateEntries);
        addAttachmentBtn.setManaged(canCreateEntries);
        renderAttachments(entry, attachmentsList, entryId);
        addAttachmentBtn.setOnAction(e -> pickAndUploadAttachment(entryId, attachmentsList));
        VBox attachmentsBox = new VBox(8, attachmentsTitle, attachmentsList, addAttachmentBtn);
        attachmentsBox.getStyleClass().addAll("card", "pos-panel");
        attachmentsBox.setPadding(new Insets(10));

        VBox content = new VBox(14,
                header,
                new Label("Lignes"),
                linesTable,
                attachmentsBox
        );
        content.setPadding(new Insets(10));
        content.setPrefWidth(560);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(560);
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    private void renderAttachments(JsonNode entry, VBox attachmentsList, long entryId) {
        attachmentsList.getChildren().clear();
        JsonNode list = entry == null ? null : entry.get("attachments");
        if (list == null || !list.isArray() || list.isEmpty()) {
            attachmentsList.getChildren().add(new Label("Aucune facture jointe."));
            return;
        }
        for (JsonNode a : list) {
            long attachmentId = a.path("id").asLong();
            String fileName = a.path("fileName").asText("fichier");
            String url = a.path("url").asText(null);
            Label nameLabel = new Label(fileName);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            Button openBtn = new Button("Ouvrir");
            openBtn.getStyleClass().addAll("button-ghost", "pos-action-sm");
            openBtn.setOnAction(e -> openAttachment(url));
            Button delBtn = new Button("Supprimer");
            delBtn.getStyleClass().addAll("button-ghost", "pos-action-sm");
            delBtn.setVisible(canCreateEntries);
            delBtn.setManaged(canCreateEntries);
            delBtn.setOnAction(e -> deleteAttachment(entryId, attachmentId, attachmentsList));
            HBox lineRow = new HBox(8, nameLabel, openBtn, delBtn);
            lineRow.setAlignment(Pos.CENTER_LEFT);
            attachmentsList.getChildren().add(lineRow);
        }
    }

    private void openAttachment(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isBlank()) {
            return;
        }
        try {
            String full = session.api().getBaseUrl() + relativeUrl;
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(full));
        } catch (Exception ex) {
            error.show("Impossible d'ouvrir la pièce jointe : " + ex.getMessage());
        }
    }

    private void deleteAttachment(long entryId, long attachmentId, VBox attachmentsList) {
        loading.setLoading(true);
        FxAsync.run(() -> {
            client.deleteEntryAttachment(entryId, attachmentId);
            return client.getEntry(entryId);
        }, node -> {
            loading.setLoading(false);
            renderAttachments(node, attachmentsList, entryId);
            reload();
        }, this::fail);
    }

    private void pickAndUploadAttachment(long entryId, VBox attachmentsList) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir la facture / le reçu");
        var file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> {
            byte[] bytes = Files.readAllBytes(file.toPath());
            client.addEntryAttachment(entryId, file.getName(), bytes);
            return client.getEntry(entryId);
        }, node -> {
            loading.setLoading(false);
            error.hide();
            renderAttachments(node, attachmentsList, entryId);
        }, this::fail);
    }

    private void openCreateEntryDialog() {
        loading.setLoading(true);
        FxAsync.run(() -> new Refs(client.listWarehouses(),
                        products.search(new com.gestpov.desktop.model.ProductQuery()), suppliers.findAll()),
                refs -> {
                    loading.setLoading(false);
                    showCreateEntryDialog(refs);
                }, this::fail);
    }

    private void showCreateEntryDialog(Refs refs) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Créer une entrée de stock");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        ComboBox<Supplier> supplierCombo = new ComboBox<>();
        supplierCombo.getItems().setAll(refs.suppliers());
        supplierCombo.setPromptText("Fournisseur (optionnel — ex. achat bazar sans fournisseur enregistré)");
        supplierCombo.setMaxWidth(Double.MAX_VALUE);

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

        DatePicker entryDate = new DatePicker(LocalDate.now());
        entryDate.setMaxWidth(Double.MAX_VALUE);
        TextField referenceField = new TextField();
        referenceField.setPromptText("N° de facture (optionnel — la photo se joint après création)");
        TextField notesField = new TextField();
        notesField.setPromptText("Notes (optionnel)");

        VBox linesBox = new VBox(8);
        record LineRow(ComboBox<ProductOption> product, TextField qty, TextField unitCost) {
        }
        List<LineRow> rows = new java.util.ArrayList<>();
        Runnable addRow = () -> {
            ComboBox<ProductOption> productCombo = new ComboBox<>();
            productCombo.getItems().setAll(refs.products());
            productCombo.setMaxWidth(220);
            TextField qtyField = new TextField("1");
            qtyField.setPromptText("Qté");
            qtyField.setPrefWidth(70);
            TextField costField = new TextField();
            costField.setPromptText("Coût unitaire (défaut : prix d'achat produit)");
            costField.setPrefWidth(100);
            productCombo.valueProperty().addListener((o, a, b) -> {
                if (b != null && b.prixAchat() != null && b.prixAchat().compareTo(BigDecimal.ZERO) > 0
                        && costField.getText().isBlank()) {
                    costField.setText(b.prixAchat().toPlainString());
                }
            });
            Button removeBtn = new Button("×");
            removeBtn.getStyleClass().add("button-ghost");
            HBox lineRow = new HBox(8, productCombo, qtyField, costField, removeBtn);
            lineRow.setAlignment(Pos.CENTER_LEFT);
            LineRow row = new LineRow(productCombo, qtyField, costField);
            removeBtn.setOnAction(ev -> {
                rows.remove(row);
                linesBox.getChildren().remove(lineRow);
            });
            rows.add(row);
            linesBox.getChildren().add(lineRow);
        };
        Button addLineBtn = new Button("+ Ajouter une ligne (autre produit)");
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
                String costText = r.unitCost().getText();
                if (costText != null && !costText.isBlank()) {
                    try {
                        line.put("unitCost", new BigDecimal(costText.trim().replace(',', '.')));
                    } catch (Exception ex) {
                        error.show("Coût unitaire invalide pour « " + p.nom() + " ».");
                        return;
                    }
                }
                lines.add(line);
            }
            if (lines.isEmpty()) {
                error.show("Ajoutez au moins une ligne de produit.");
                return;
            }
            Long supplierId = supplierCombo.getValue() == null ? null : supplierCombo.getValue().id();
            LocalDate date = entryDate.getValue();
            String reference = referenceField.getText();
            String notes = notesField.getText();
            loading.setLoading(true);
            FxAsync.run(() -> client.createEntry(supplierId, wh.id(), loc.id(), date, reference, notes, lines),
                    created -> {
                        loading.setLoading(false);
                        error.hide();
                        dialog.close();
                        Alert done = new Alert(Alert.AlertType.INFORMATION,
                                "Entrée créée en brouillon — sélectionnez-la (double-clic) pour joindre la "
                                        + "facture puis cliquez « Valider » pour incrémenter le stock.");
                        done.setHeaderText("Bon d'entrée créé");
                        done.showAndWait();
                        tabEntries.setSelected(true);
                        showSelectedTab();
                        reload();
                    }, t -> {
                        loading.setLoading(false);
                        fail(t);
                    });
        });

        VBox content = new VBox(10,
                labeled("Fournisseur", supplierCombo),
                labeled("Entrepôt", whCombo),
                labeled("Emplacement", locCombo),
                labeled("Date d'entrée", entryDate),
                labeled("Référence facture", referenceField),
                new Label("Lignes (une par produit)"),
                linesBox,
                addLineBtn,
                labeled("Notes", notesField),
                confirm
        );
        content.setPadding(new Insets(10));
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    // ---- Sorties ----

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

    private void showExitDetail(long id) {
        loading.setLoading(true);
        FxAsync.run(() -> client.getExit(id), node -> {
            loading.setLoading(false);
            openExitDetailDialog(node);
        }, this::fail);
    }

    private void openExitDetailDialog(JsonNode exit) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Sortie " + exit.path("exitNumber").asText("—"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());

        GridPane header = detailGrid();
        int row = 0;
        row = addDetailRow(header, row, "N°", exit.path("exitNumber").asText("—"));
        row = addDetailRow(header, row, "Date", exit.path("exitDate").asText("—"));
        row = addDetailRow(header, row, "Motif", reasonLabel(exit.path("reason").asText(null)));
        row = addDetailRow(header, row, "Entrepôt", textOr(exit, "warehouseCode", "—")
                + " / " + textOr(exit, "locationCode", "—"));
        row = addDetailRow(header, row, "Statut", statusLabel(exit.path("status").asText(null)));
        if (exit.path("posOrigin").asBoolean(false)) {
            row = addDetailRow(header, row, "Vente d'origine", textOr(exit, "saleNumber", "—"));
        }
        row = addDetailRow(header, row, "Créé par", textOr(exit, "createdBy", "—"));
        if (exit.hasNonNull("validatedBy")) {
            row = addDetailRow(header, row, "Validé par", exit.path("validatedBy").asText("—"));
        }
        if (exit.hasNonNull("cancelledBy")) {
            row = addDetailRow(header, row, "Annulé par", exit.path("cancelledBy").asText("—"));
        }
        if (exit.hasNonNull("notes") && !exit.path("notes").asText("").isBlank()) {
            addDetailRow(header, row, "Notes", exit.path("notes").asText());
        }

        TableView<JsonNode> linesTable = new TableView<>();
        linesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        linesTable.setPlaceholder(new EmptyState("Aucune ligne"));
        linesTable.getColumns().addAll(
                lineCol("Produit", l -> textOr(l, "productNom", "—")),
                lineCol("Quantité", l -> textOr(l, "quantityInput", "—") + " " + textOr(l, "unitSymbole", "")),
                lineCol("Notes", l -> textOr(l, "notes", ""))
        );
        JsonNode lignes = exit.get("lignes");
        if (lignes != null && lignes.isArray()) {
            lignes.forEach(linesTable.getItems()::add);
        }
        linesTable.setPrefHeight(Math.min(260, 40 + linesTable.getItems().size() * 28));

        VBox content = new VBox(14, header, new Label("Lignes"), linesTable);
        content.setPadding(new Insets(10));
        content.setPrefWidth(520);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(480);
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    private void openCreateExitDialog() {
        loading.setLoading(true);
        FxAsync.run(() -> new Refs(client.listWarehouses(),
                        products.search(new com.gestpov.desktop.model.ProductQuery()), List.of()),
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
            HBox lineRow = new HBox(8, productCombo, qtyField, removeBtn);
            lineRow.setAlignment(Pos.CENTER_LEFT);
            LineRow row = new LineRow(productCombo, qtyField);
            removeBtn.setOnAction(ev -> {
                rows.remove(row);
                linesBox.getChildren().remove(lineRow);
            });
            rows.add(row);
            linesBox.getChildren().add(lineRow);
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

    private static GridPane detailGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(130);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);
        return grid;
    }

    private static int addDetailRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        Label v = new Label(value == null || value.isBlank() ? "—" : value);
        v.setWrapText(true);
        grid.addRow(row, l, v);
        return row + 1;
    }

    private static TableColumn<JsonNode, String> lineCol(String title,
                                                        java.util.function.Function<JsonNode, String> fn) {
        TableColumn<JsonNode, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static String textOr(JsonNode node, String field, String fallback) {
        if (node == null || !node.hasNonNull(field)) {
            return fallback;
        }
        String text = node.get(field).asText();
        return text == null || text.isBlank() ? fallback : text;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    // yyyy-MM-dd (pas dd/MM/yyyy) : le tri de la colonne compare le texte affiché, un format
    // ISO reste trie-able correctement même à cheval sur un changement de mois/année.
    private static final java.time.format.DateTimeFormatter DATE_TIME_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Date + heure de création (triable chronologiquement) ; repli sur la date métier seule si absente. */
    private static String dateTime(String createdAtIso, String fallbackDate) {
        if (createdAtIso != null && !createdAtIso.isBlank()) {
            try {
                java.time.Instant instant = java.time.Instant.parse(createdAtIso);
                return DATE_TIME_FORMAT.format(instant.atZone(java.time.ZoneId.systemDefault()));
            } catch (Exception ignored) {
                // repli ci-dessous
            }
        }
        return nz(fallbackDate);
    }

    private static String statusLabel(String status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case "DRAFT" -> "Brouillon";
            case "VALIDATED" -> "Validée";
            case "CANCELLED" -> "Annulée";
            default -> status;
        };
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

    private record Refs(List<Warehouse> warehouses, List<Product> productsRaw, List<Supplier> suppliers) {
        List<ProductOption> products() {
            return productsRaw.stream().map(p -> new ProductOption(p.id(), p.nom(), p.sku(), p.prixAchat())).toList();
        }
    }

    private record ProductOption(Long id, String nom, String sku, BigDecimal prixAchat) {
        @Override
        public String toString() {
            return sku == null || sku.isBlank() ? nom : nom + " (" + sku + ")";
        }
    }
}
