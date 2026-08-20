package com.gestpov.desktop.ui.products;

import com.gestpov.desktop.model.Brand;
import com.gestpov.desktop.model.Category;
import com.gestpov.desktop.model.PriceHistory;
import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.ProductDraft;
import com.gestpov.desktop.model.ProductImage;
import com.gestpov.desktop.model.Supplier;
import com.gestpov.desktop.model.Unit;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.BrandClient;
import com.gestpov.desktop.net.CategoryClient;
import com.gestpov.desktop.net.ProductClient;
import com.gestpov.desktop.net.SupplierClient;
import com.gestpov.desktop.net.UnitClient;
import com.gestpov.desktop.service.ProductValidator;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ConfirmationDialog;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class ProductFormView extends StackPane {

    private final SessionContext session;
    private final ProductClient products;
    private final CategoryClient categoriesApi;
    private final BrandClient brandsApi;
    private final SupplierClient suppliersApi;
    private final UnitClient unitsApi;
    private final Long productId;
    private final Runnable onBack;
    private final java.util.function.LongConsumer onCreated;
    private final ErrorBanner errorBanner = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TextField nom = new TextField();
    private final TextField sku = new TextField();
    private final TextField codeBarre = new TextField();
    private final CheckBox generateBarcode = new CheckBox("Générer un EAN-13 à l'enregistrement (activé par défaut)");
    private final ComboBox<RefOption> marque = new ComboBox<>();
    private final ComboBox<RefOption> categorie = new ComboBox<>();
    private final ComboBox<RefOption> fournisseur = new ComboBox<>();
    private final ComboBox<RefOption> unite = new ComboBox<>();
    private final TextField prixAchat = new TextField();
    private final TextField prixVente = new TextField();
    private final ComboBox<RefOption> statut = new ComboBox<>();
    private final ComboBox<RefOption> cycleVie = new ComboBox<>();
    private final TextArea description = new TextArea();
    private final Label stockBadge = new Label();
    private final Button saveButton = new Button("Enregistrer");
    private final ComboBox<String> priceType = new ComboBox<>();
    private final TextField newPrice = new TextField();
    private final TableView<PriceHistory> historyTable = new TableView<>();
    private final FlowPane imagePane = new FlowPane(8, 8);
    private Product current;
    private final TabPane tabs = new TabPane();
    private final Tab pricesTab = new Tab("Prix");
    private final Tab imagesTab = new Tab("Images");

    public ProductFormView(SessionContext session, Long productId, Runnable onBack,
                           java.util.function.LongConsumer onCreated) {
        this.session = session;
        this.productId = productId;
        this.onBack = onBack;
        this.onCreated = onCreated;
        this.products = new ProductClient(session.api());
        this.categoriesApi = new CategoryClient(session.api());
        this.brandsApi = new BrandClient(session.api());
        this.suppliersApi = new SupplierClient(session.api());
        this.unitsApi = new UnitClient(session.api());
        getChildren().addAll(buildContent(), loading);
        load();
    }

    private javafx.scene.Node buildContent() {
        boolean isNew = productId == null;
        Label title = new Label(isNew ? "Nouveau produit" : "Fiche produit");
        title.getStyleClass().add("page-title");
        Button back = new Button("Retour à la liste");
        back.getStyleClass().add("button-ghost");
        back.setOnAction(e -> onBack.run());
        stockBadge.getStyleClass().add("badge");
        HBox header = new HBox(12, back, title, stockBadge);
        header.setAlignment(Pos.CENTER_LEFT);

        saveButton.getStyleClass().add("button-primary");
        saveButton.setOnAction(e -> save());
        boolean canWrite = isNew ? session.hasPermission("products.create") : session.hasPermission("products.update");
        saveButton.setVisible(canWrite);
        saveButton.setManaged(canWrite);

        Tab general = new Tab("Général", generalForm());
        general.setClosable(false);
        pricesTab.setClosable(false);
        pricesTab.setContent(pricesPane());
        imagesTab.setClosable(false);
        imagesTab.setContent(imagesPane());
        tabs.getTabs().add(general);
        if (!isNew) {
            tabs.getTabs().addAll(pricesTab, imagesTab);
        }
        tabs.getStyleClass().add("product-tabs");

        VBox page = new VBox(16, header, errorBanner, tabs, saveButton);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
    }

    private GridPane generalForm() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.getStyleClass().add("card");
        int r = 0;
        nom.setPromptText("Nom");
        sku.setPromptText("Auto depuis le nom si vide");
        codeBarre.setPromptText("EAN-13 ou génération auto");
        generateBarcode.setSelected(productId == null);
        description.setPrefRowCount(3);
        for (String s : ProductLabels.STATUTS) {
            statut.getItems().add(new RefOption(s, ProductLabels.status(s)));
        }
        for (String s : ProductLabels.CYCLES) {
            cycleVie.getItems().add(new RefOption(s, ProductLabels.lifecycle(s)));
        }
        statut.getSelectionModel().selectFirst();
        cycleVie.getSelectionModel().selectFirst();
        addField(grid, r++, 0, "Nom", nom);
        addField(grid, r - 1, 1, "SKU", sku);
        addField(grid, r++, 0, "Code-barres (produit simple)", codeBarre, 2);
        grid.add(generateBarcode, 0, r++, 2, 1);
        addField(grid, r, 0, "Marque", marque);
        addField(grid, r++, 1, "Catégorie", categorie);
        addField(grid, r, 0, "Fournisseur principal", fournisseur);
        addField(grid, r++, 1, "Unité de base (stock)", unite);
        addField(grid, r, 0, "Prix achat", prixAchat);
        addField(grid, r++, 1, "Prix vente", prixVente);
        addField(grid, r, 0, "Statut", statut);
        addField(grid, r++, 1, "Cycle de vie", cycleVie);
        addField(grid, r, 0, "Description", description, 2);
        Label unitHint = new Label("Le stock est toujours exprimé dans cette unité.");
        unitHint.getStyleClass().add("page-sub");
        grid.add(unitHint, 1, r - 3);
        return grid;
    }

    private VBox pricesPane() {
        priceType.getItems().setAll(ProductLabels.PRICE_TYPES);
        priceType.getSelectionModel().select("VENTE");
        newPrice.setPromptText("Nouveau prix");
        Button apply = new Button("Appliquer");
        apply.getStyleClass().add("button-primary");
        apply.setOnAction(e -> applyPrice());
        apply.setVisible(session.hasPermission("products.update"));
        apply.setManaged(session.hasPermission("products.update"));
        HBox form = new HBox(8, labeled("Type", priceType), labeled("Nouveau prix", newPrice), apply);
        form.setAlignment(Pos.BOTTOM_LEFT);
        form.getStyleClass().add("card");

        TableColumn<PriceHistory, String> date = col("Date", h -> ProductLabels.date(h.dateModification()));
        TableColumn<PriceHistory, String> type = col("Type", PriceHistory::type);
        TableColumn<PriceHistory, String> oldP = col("Ancien", h -> ProductLabels.price(h.ancienPrix()));
        TableColumn<PriceHistory, String> newP = col("Nouveau", h -> ProductLabels.price(h.nouveauPrix()));
        TableColumn<PriceHistory, String> user = col("Utilisateur", h -> h.utilisateur() == null ? "—" : h.utilisateur());
        historyTable.getColumns().addAll(date, type, oldP, newP, user);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setPrefHeight(240);
        VBox box = new VBox(12, form, historyTable);
        return box;
    }

    private VBox imagesPane() {
        Button upload = new Button("Ajouter une image");
        upload.getStyleClass().add("button-secondary");
        upload.setOnAction(e -> uploadImage());
        upload.setVisible(session.hasPermission("products.create"));
        upload.setManaged(session.hasPermission("products.create"));
        imagePane.getStyleClass().add("card");
        VBox box = new VBox(12, upload, imagePane);
        return box;
    }

    private void load() {
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> {
            FormData data = new FormData();
            data.categories = categoriesApi.getTree();
            data.brands = brandsApi.findAll();
            data.suppliers = suppliersApi.findAll();
            data.units = unitsApi.findAll();
            if (productId != null) {
                data.product = products.getById(productId);
                data.history = products.priceHistory(productId);
            }
            return data;
        }, this::bind, this::showError);
    }

    private void bind(FormData data) {
        setBusy(false);
        fillRefs(marque, data.brands.stream().map(b -> new RefOption(String.valueOf(b.id()), b.nom())).toList());
        List<RefOption> cats = new ArrayList<>();
        flatten(data.categories, "", cats);
        fillRefs(categorie, cats);
        fillRefs(fournisseur, data.suppliers.stream()
                .map(s -> new RefOption(String.valueOf(s.id()), s.nom())).toList());
        fillRefs(unite, data.units.stream()
                .map(u -> new RefOption(String.valueOf(u.id()), u.toString())).toList());
        current = data.product;
        if (current == null) {
            stockBadge.setText("");
            return;
        }
        nom.setText(nullToEmpty(current.nom()));
        sku.setText(nullToEmpty(current.sku()));
        codeBarre.setText(nullToEmpty(current.codeBarre()));
        generateBarcode.setSelected(false);
        description.setText(nullToEmpty(current.description()));
        prixAchat.setText(decimalText(current.prixAchat()));
        prixVente.setText(decimalText(current.prixVente()));
        select(marque, current.marqueId());
        select(categorie, current.categorieId());
        select(fournisseur, current.fournisseurPrincipalId());
        select(unite, current.unitId());
        selectCode(statut, current.statut());
        selectCode(cycleVie, current.cycleVie());
        stockBadge.setText("Stock: " + current.stockLabel());
        if (current.hasVariants()) {
            codeBarre.setDisable(true);
            generateBarcode.setDisable(true);
        }
        historyTable.getItems().setAll(data.history);
        renderImages();
    }

    private void save() {
        String nameError = ProductValidator.validateName(nom.getText());
        if (nameError != null) {
            errorBanner.show(nameError);
            return;
        }
        String priceError = firstNonNull(
                ProductValidator.validateOptionalPrice(prixAchat.getText()),
                ProductValidator.validateOptionalPrice(prixVente.getText()));
        if (priceError != null) {
            errorBanner.show(priceError);
            return;
        }
        ProductDraft draft = readDraft();
        errorBanner.hide();
        setBusy(true);
        if (productId == null) {
            FxAsync.run(() -> products.create(draft), created -> {
                setBusy(false);
                if (created != null && created.id() != null && onCreated != null) {
                    onCreated.accept(created.id());
                } else {
                    onBack.run();
                }
            }, this::showError);
        } else {
            FxAsync.run(() -> products.update(productId, draft), updated -> {
                setBusy(false);
                load();
            }, this::showError);
        }
    }

    private ProductDraft readDraft() {
        ProductDraft draft = ProductDraft.from(current);
        draft.nom = ProductValidator.normalize(nom.getText());
        draft.sku = ProductValidator.normalize(sku.getText());
        draft.codeBarre = ProductValidator.normalize(codeBarre.getText());
        draft.generateBarcode = generateBarcode.isSelected();
        draft.description = ProductValidator.normalize(description.getText());
        draft.marqueId = selectedId(marque);
        draft.categorieId = selectedId(categorie);
        draft.fournisseurPrincipalId = selectedId(fournisseur);
        draft.unitId = selectedId(unite);
        draft.prixAchat = silentPrice(prixAchat.getText());
        draft.prixVente = silentPrice(prixVente.getText());
        draft.statut = selectedCode(statut, "ACTIF");
        draft.cycleVie = selectedCode(cycleVie, "BROUILLON");
        return draft;
    }

    private void applyPrice() {
        String error = ProductValidator.validateRequiredPrice(newPrice.getText());
        if (error != null) {
            errorBanner.show(error);
            return;
        }
        BigDecimal value = ProductValidator.parsePrice(newPrice.getText());
        String type = priceType.getValue() == null ? "VENTE" : priceType.getValue();
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> products.updatePrice(productId, type, value), updated -> {
            newPrice.clear();
            load();
        }, this::showError);
    }

    private void uploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Image produit");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        java.io.File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        setBusy(true);
        boolean principale = current == null || current.images() == null || current.images().isEmpty();
        FxAsync.run(() -> {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return products.uploadImage(productId, file.getName(), bytes, principale);
        }, ignored -> load(), this::showError);
    }

    private void renderImages() {
        imagePane.getChildren().clear();
        if (current == null || current.images() == null || current.images().isEmpty()) {
            Label empty = new Label("Aucune image");
            empty.getStyleClass().add("empty-state");
            imagePane.getChildren().add(empty);
            return;
        }
        for (ProductImage image : current.images()) {
            imagePane.getChildren().add(imageCard(image));
        }
    }

    private VBox imageCard(ProductImage image) {
        ImageView view = new ImageView();
        view.setFitWidth(120);
        view.setFitHeight(120);
        view.setPreserveRatio(true);
        String url = resolveUrl(image.url());
        if (url != null) {
            try {
                view.setImage(new Image(url, 120, 120, true, true, true));
            } catch (Exception ignored) {
                // nom de fichier ci-dessous
            }
        }
        Label name = new Label(image.fileName() == null ? "" : image.fileName());
        name.setWrapText(true);
        name.setMaxWidth(120);
        VBox box = new VBox(6, view, name);
        if (image.principale()) {
            Label main = new Label("Principale");
            main.getStyleClass().add("badge");
            box.getChildren().add(main);
        }
        if (session.hasPermission("products.delete")) {
            Button del = new Button("Suppr.");
            del.getStyleClass().add("button-danger");
            del.setOnAction(e -> deleteImage(image));
            box.getChildren().add(del);
        }
        box.getStyleClass().add("image-card");
        return box;
    }

    private void deleteImage(ProductImage image) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer l'image", "Supprimer cette image ?")) {
            return;
        }
        setBusy(true);
        FxAsync.runVoid(() -> products.deleteImage(productId, image.id()), this::load, this::showError);
    }

    private String resolveUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return session.api().getBaseUrl() + (url.startsWith("/") ? url : "/" + url);
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
        saveButton.setDisable(busy);
    }

    private static void addField(GridPane grid, int row, int col, String label, javafx.scene.Node node) {
        addField(grid, row, col, label, node, 1);
    }

    private static void addField(GridPane grid, int row, int col, String label, javafx.scene.Node node, int span) {
        VBox box = labeled(label, node);
        grid.add(box, col, row, span, 1);
        GridPane.setHgrow(box, Priority.ALWAYS);
    }

    private static VBox labeled(String label, javafx.scene.Node node) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        VBox box = new VBox(4, l, node);
        return box;
    }

    private static TableColumn<PriceHistory, String> col(String title, java.util.function.Function<PriceHistory, String> fn) {
        TableColumn<PriceHistory, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue() == null ? "" : fn.apply(data.getValue())));
        return col;
    }

    private static void fillRefs(ComboBox<RefOption> combo, List<RefOption> items) {
        combo.getItems().setAll(RefOption.none());
        combo.getItems().addAll(items);
        combo.getSelectionModel().selectFirst();
    }

    private static void flatten(List<Category> cats, String prefix, List<RefOption> out) {
        if (cats == null) {
            return;
        }
        for (Category cat : cats) {
            String label = prefix.isEmpty() ? cat.nom() : prefix + " > " + cat.nom();
            out.add(new RefOption(String.valueOf(cat.id()), label));
            flatten(cat.childrenOrEmpty(), label, out);
        }
    }

    private static void select(ComboBox<RefOption> combo, Long id) {
        if (id == null) {
            combo.getSelectionModel().selectFirst();
            return;
        }
        combo.getItems().stream()
                .filter(o -> o.id != null && o.id.equals(String.valueOf(id)))
                .findFirst()
                .ifPresent(combo.getSelectionModel()::select);
    }

    private static void selectCode(ComboBox<RefOption> combo, String code) {
        if (code == null) {
            return;
        }
        combo.getItems().stream()
                .filter(o -> code.equals(o.id))
                .findFirst()
                .ifPresent(combo.getSelectionModel()::select);
    }

    private static Long selectedId(ComboBox<RefOption> combo) {
        RefOption option = combo.getValue();
        if (option == null || option.id == null) {
            return null;
        }
        return Long.parseLong(option.id);
    }

    private static String selectedCode(ComboBox<RefOption> combo, String fallback) {
        RefOption option = combo.getValue();
        return option == null || option.id == null ? fallback : option.id;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String decimalText(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static BigDecimal silentPrice(String raw) {
        try {
            return ProductValidator.parsePrice(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class FormData {
        List<Category> categories = List.of();
        List<Brand> brands = List.of();
        List<Supplier> suppliers = List.of();
        List<Unit> units = List.of();
        Product product;
        List<PriceHistory> history = List.of();
    }

    static final class RefOption {
        final String id;
        final String label;

        RefOption(String id, String label) {
            this.id = id;
            this.label = label;
        }

        static RefOption none() {
            return new RefOption(null, "—");
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
