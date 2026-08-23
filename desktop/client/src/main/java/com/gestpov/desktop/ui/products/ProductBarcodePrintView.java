package com.gestpov.desktop.ui.products;

import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.ProductQuery;
import com.gestpov.desktop.model.ProductVariant;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.BarcodeClient;
import com.gestpov.desktop.net.ProductClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.Reloadable;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Écran d'impression d'étiquettes code-barres pour produits/variantes — recherche, file
 * d'attente avec quantités, mise en page en grille et envoi à l'imprimante système.
 */
public final class ProductBarcodePrintView extends StackPane implements Reloadable {

    private static final int COLS = 3;
    private static final int ROWS = 8;
    private static final int PER_PAGE = COLS * ROWS;

    private final SessionContext session;
    private final ProductClient products;
    private final BarcodeClient barcodes;
    private final ErrorBanner errorBanner = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();

    private final TextField searchField = new TextField();
    private final TableView<Product> resultsTable = new TableView<>();
    private final TableView<PrintLine> queueTable = new TableView<>();
    private final Label queueSummary = new Label();
    private final CheckBox showName = new CheckBox("Afficher le nom du produit");
    private final CheckBox showPrice = new CheckBox("Afficher le prix");
    private final Button printButton = new Button("Imprimer les étiquettes");

    public ProductBarcodePrintView(SessionContext session) {
        this.session = session;
        this.products = new ProductClient(session.api());
        this.barcodes = new BarcodeClient(session.api());
        getChildren().addAll(build(), loading);
        search();
    }

    /** Ajoute des produits déjà sélectionnés ailleurs (ex: liste Produits) à la file d'impression. */
    public void queueProducts(List<Product> preselected) {
        if (preselected == null || preselected.isEmpty()) {
            return;
        }
        for (Product p : preselected) {
            addProduct(p);
        }
    }

    private VBox build() {
        Label title = new Label("Étiquettes codes-barres");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Recherchez des produits, ajustez les quantités puis imprimez les étiquettes à coller.");
        sub.getStyleClass().add("page-sub");

        searchField.setPromptText("Rechercher par nom ou SKU (ou scanner un code-barres existant)");
        searchField.setOnAction(e -> search());
        Button searchBtn = new Button("Rechercher");
        searchBtn.getStyleClass().add("button-secondary");
        searchBtn.setOnAction(e -> search());
        HBox searchRow = new HBox(8, searchField, searchBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        resultsTable.setPlaceholder(new EmptyState("Aucun produit"));
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        resultsTable.setPrefHeight(220);
        resultsTable.getColumns().add(colP("Produit", p -> p.nom() == null ? "" : p.nom()));
        resultsTable.getColumns().add(colP("SKU", p -> p.sku() == null ? "" : p.sku()));
        resultsTable.getColumns().add(colP("Code-barres", p ->
                p.hasVariants() ? "Variantes" : (p.codeBarre() == null || p.codeBarre().isBlank() ? "—" : p.codeBarre())));
        resultsTable.getColumns().add(colP("Prix vente", p -> ProductLabels.price(p.prixVente())));
        TableColumn<Product, Void> addCol = new TableColumn<>();
        addCol.setPrefWidth(90);
        addCol.setSortable(false);
        addCol.setCellFactory(c -> new TableCell<>() {
            private final Button add = new Button("+ Ajouter");

            {
                add.getStyleClass().addAll("button-primary", "pos-action-sm");
                add.setOnAction(e -> {
                    Product product = getTableRow() == null ? null : getTableRow().getItem();
                    if (product != null) {
                        addProduct(product);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : add);
            }
        });
        resultsTable.getColumns().add(addCol);

        VBox searchCard = new VBox(10, searchRow, resultsTable);
        searchCard.getStyleClass().add("card");

        Label queueTitle = new Label("FILE D'IMPRESSION");
        queueTitle.getStyleClass().add("section-title");
        queueSummary.getStyleClass().add("page-sub");

        queueTable.setPlaceholder(new EmptyState("Ajoutez des produits ci-dessus pour composer la planche d'étiquettes"));
        queueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        queueTable.setPrefHeight(260);
        queueTable.getColumns().add(colL("Produit", l -> l.title));
        queueTable.getColumns().add(colL("Code-barres", l -> l.key));
        queueTable.getColumns().add(colL("Prix", l -> l.price == null ? "—" : ProductLabels.price(l.price)));
        TableColumn<PrintLine, Integer> qtyCol = new TableColumn<>("Quantité");
        qtyCol.setPrefWidth(110);
        qtyCol.setCellFactory(c -> new TableCell<>() {
            private final TextField field = new TextField();

            {
                field.setPrefWidth(70);
                field.setOnAction(e -> commit());
                field.focusedProperty().addListener((o, was, is) -> {
                    if (!is) {
                        commit();
                    }
                });
            }

            private void commit() {
                PrintLine line = getTableRow() == null ? null : getTableRow().getItem();
                if (line == null) {
                    return;
                }
                try {
                    int value = Math.max(1, Integer.parseInt(field.getText().trim()));
                    line.quantity.set(value);
                } catch (NumberFormatException ex) {
                    field.setText(String.valueOf(line.quantity.get()));
                }
                updateQueueSummary();
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                field.setText(String.valueOf(getTableRow().getItem().quantity.get()));
                setGraphic(field);
            }
        });
        queueTable.getColumns().add(qtyCol);
        TableColumn<PrintLine, Void> removeCol = new TableColumn<>();
        removeCol.setPrefWidth(70);
        removeCol.setSortable(false);
        removeCol.setCellFactory(c -> new TableCell<>() {
            private final Button del = new Button("×");

            {
                del.getStyleClass().add("button-ghost");
                del.setOnAction(e -> {
                    PrintLine line = getTableRow() == null ? null : getTableRow().getItem();
                    if (line != null) {
                        queueTable.getItems().remove(line);
                        updateQueueSummary();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : del);
            }
        });
        queueTable.getColumns().add(removeCol);

        showName.setSelected(true);
        HBox options = new HBox(20, showName, showPrice);
        options.setAlignment(Pos.CENTER_LEFT);

        printButton.getStyleClass().addAll("button-primary", "button-lg");
        printButton.setOnAction(e -> print());

        HBox printRow = new HBox(16, queueSummary, printButton);
        printRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(queueSummary, Priority.ALWAYS);

        VBox queueCard = new VBox(12, queueTitle, queueTable, options, printRow);
        queueCard.getStyleClass().add("card");

        updateQueueSummary();

        VBox page = new VBox(18, title, sub, errorBanner, searchCard, queueCard);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void addProduct(Product product) {
        if (product == null || product.id() == null) {
            return;
        }
        if (!product.hasVariants()) {
            if (product.codeBarre() == null || product.codeBarre().isBlank()) {
                errorBanner.show("« " + product.nom() + " » n'a pas de code-barres — générez-en un dans sa fiche.");
                return;
            }
            addLine(product.codeBarre(), product.nom(), product.prixVente());
            return;
        }
        setBusy(true);
        FxAsync.run(() -> products.listVariants(product.id()), variants -> {
            setBusy(false);
            int added = 0;
            for (ProductVariant v : variants) {
                if (v.codeBarre() == null || v.codeBarre().isBlank()) {
                    continue;
                }
                String label = product.nom() + (v.label() == null || v.label().isBlank() ? "" : " — " + v.label());
                BigDecimal price = v.prix() != null ? v.prix() : product.prixVente();
                addLine(v.codeBarre(), label, price);
                added++;
            }
            if (added == 0) {
                errorBanner.show("Aucune variante de « " + product.nom() + " » n'a de code-barres.");
            }
        }, this::showError);
    }

    private void addLine(String key, String title, BigDecimal price) {
        for (PrintLine existing : queueTable.getItems()) {
            if (existing.key.equals(key)) {
                existing.quantity.set(existing.quantity.get() + 1);
                queueTable.refresh();
                updateQueueSummary();
                return;
            }
        }
        queueTable.getItems().add(new PrintLine(key, title, price));
        updateQueueSummary();
    }

    private void updateQueueSummary() {
        int lines = queueTable.getItems().size();
        int total = queueTable.getItems().stream().mapToInt(l -> l.quantity.get()).sum();
        queueSummary.setText(lines == 0 ? "File vide" : lines + " référence(s) · " + total + " étiquette(s) au total");
    }

    private void search() {
        errorBanner.hide();
        setBusy(true);
        ProductQuery q = new ProductQuery();
        q.query = searchField.getText();
        FxAsync.run(() -> products.search(q), list -> {
            setBusy(false);
            resultsTable.getItems().setAll(list);
        }, this::showError);
    }

    private void print() {
        if (queueTable.getItems().isEmpty()) {
            errorBanner.show("Ajoutez au moins un produit à la file d'impression.");
            return;
        }
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> {
            Map<String, byte[]> images = new LinkedHashMap<>();
            for (PrintLine line : queueTable.getItems()) {
                if (!images.containsKey(line.key)) {
                    String base64 = barcodes.generateBase64(line.key, "EAN13");
                    images.put(line.key, base64 == null ? null : Base64.getDecoder().decode(base64));
                }
            }
            return images;
        }, this::doPrint, this::showError);
    }

    private void doPrint(Map<String, byte[]> images) {
        setBusy(false);
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            errorBanner.show("Aucune imprimante disponible sur ce poste.");
            return;
        }
        if (!job.showPrintDialog(getScene() == null ? null : getScene().getWindow())) {
            return;
        }
        Printer printer = job.getPrinter();
        PageLayout layout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
        job.getJobSettings().setPageLayout(layout);

        List<PrintLine> expanded = new ArrayList<>();
        for (PrintLine line : queueTable.getItems()) {
            int qty = Math.max(1, line.quantity.get());
            for (int i = 0; i < qty; i++) {
                expanded.add(line);
            }
        }
        boolean ok = true;
        for (int start = 0; start < expanded.size(); start += PER_PAGE) {
            List<PrintLine> chunk = expanded.subList(start, Math.min(start + PER_PAGE, expanded.size()));
            GridPane page = buildPage(chunk, images, layout.getPrintableWidth(), layout.getPrintableHeight());
            ok = job.printPage(layout, page) && ok;
        }
        if (ok) {
            job.endJob();
            Alert done = new Alert(Alert.AlertType.INFORMATION,
                    expanded.size() + " étiquette(s) envoyée(s) à l'impression.");
            done.setHeaderText("Impression lancée");
            done.showAndWait();
        } else {
            errorBanner.show("L'impression a échoué.");
        }
    }

    private GridPane buildPage(List<PrintLine> chunk, Map<String, byte[]> images, double width, double height) {
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPrefSize(width, height);
        grid.setMinSize(width, height);
        grid.setMaxSize(width, height);
        for (int c = 0; c < COLS; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / COLS);
            grid.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < ROWS; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / ROWS);
            grid.getRowConstraints().add(rc);
        }
        for (int i = 0; i < chunk.size(); i++) {
            PrintLine line = chunk.get(i);
            VBox card = labelCard(line, images.get(line.key));
            grid.add(card, i % COLS, i / COLS);
        }
        grid.applyCss();
        grid.layout();
        return grid;
    }

    private VBox labelCard(PrintLine line, byte[] imageBytes) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-border-color: #cbd5e1; -fx-border-width: 0.75; -fx-padding: 3;");
        if (showName.isSelected()) {
            Label name = new Label(line.title);
            name.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-alignment: center;");
            name.setWrapText(true);
            name.setAlignment(Pos.CENTER);
            box.getChildren().add(name);
        }
        if (imageBytes != null) {
            try {
                ImageView iv = new ImageView(new Image(new ByteArrayInputStream(imageBytes)));
                iv.setFitWidth(140);
                iv.setFitHeight(46);
                iv.setPreserveRatio(true);
                box.getChildren().add(iv);
            } catch (Exception ignored) {
                // image indisponible : le code texte suffit
            }
        }
        Label code = new Label(line.key);
        code.setStyle("-fx-font-size: 7.5px; -fx-font-family: 'Consolas', monospace;");
        box.getChildren().add(code);
        if (showPrice.isSelected() && line.price != null) {
            Label price = new Label(ProductLabels.price(line.price));
            price.setStyle("-fx-font-size: 8px; -fx-font-weight: bold;");
            box.getChildren().add(price);
        }
        return box;
    }

    @Override
    public void reload() {
        search();
    }

    private void showError(Throwable error) {
        setBusy(false);
        if (error instanceof ApiException api && api.isUnauthorized()) {
            return;
        }
        if (error instanceof ApiException api) {
            errorBanner.show(ApiException.userMessage(api));
        } else {
            errorBanner.show("Une erreur est survenue.");
        }
    }

    private void setBusy(boolean busy) {
        loading.setLoading(busy);
        printButton.setDisable(busy);
    }

    private static TableColumn<Product, String> colP(String title, java.util.function.Function<Product, String> fn) {
        TableColumn<Product, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static TableColumn<PrintLine, String> colL(String title, java.util.function.Function<PrintLine, String> fn) {
        TableColumn<PrintLine, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }

    private static final class PrintLine {
        final String key;
        final String title;
        final BigDecimal price;
        final SimpleIntegerProperty quantity = new SimpleIntegerProperty(1);

        PrintLine(String key, String title, BigDecimal price) {
            this.key = key;
            this.title = title;
            this.price = price;
        }
    }
}
