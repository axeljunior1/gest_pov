package com.gestpov.desktop.ui.stock;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.StockEntryDoc;
import com.gestpov.desktop.model.StockExitDoc;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.StockClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Liste des bons d'entrée et de sortie stock.
 */
public final class StockEntriesExitsView extends StackPane implements Reloadable {

    private final StockClient client;
    private final boolean canReadEntries;
    private final boolean canReadExits;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();

    private final ToggleButton tabEntries = new ToggleButton("Entrées");
    private final ToggleButton tabExits = new ToggleButton("Sorties");
    private final TableView<StockEntryDoc> entriesTable = new TableView<>();
    private final TableView<StockExitDoc> exitsTable = new TableView<>();
    private final VBox entriesPane = new VBox(8);
    private final VBox exitsPane = new VBox(8);

    public StockEntriesExitsView(SessionContext session) {
        this.client = new StockClient(session.api());
        this.canReadEntries = session.hasPermission("stock_entry.read");
        this.canReadExits = session.hasPermission("stock_exit.read");
        getChildren().addAll(build(), loading);
        showSelectedTab();
        reload();
    }

    private VBox build() {
        Label title = new Label("Entrées / Sorties");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Bons d'entrée et de sortie (lecture)");
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
                colX("Motif", x -> nz(x.reason())),
                colX("Entrepôt", x -> nz(x.warehouseCode())),
                colX("Statut", x -> nz(x.status()))
        );
        VBox.setVgrow(exitsTable, Priority.ALWAYS);
        exitsPane.getChildren().setAll(exitsTable);

        StackPane body = new StackPane(entriesPane, exitsPane);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox page = new VBox(12, title, sub, error, new HBox(12, tabBar, refresh), body);
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
            }, this::fail);
        }
    }

    private void fail(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api && !api.isUnauthorized()) {
            error.show(ApiException.userMessage(api));
        } else if (!(t instanceof ApiException a && a.isUnauthorized())) {
            error.show("Une erreur est survenue.");
        }
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
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
}
